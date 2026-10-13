import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import {
  Sparkles,
  UploadCloud,
  X,
  AlertTriangle,
  Search,
  CheckCircle,
  Building,
  ArrowLeft,
  ArrowRight,
  ShieldAlert,
} from 'lucide-react';
import { itemsApi } from '../api/items';
import { referenceApi } from '../api/reference';
import { uploadsApi } from '../api/uploads';
import { aiApi } from '../api/ai';
import type { ItemType } from '../types';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';

const postItemSchema = z
  .object({
    type: z.enum(['LOST', 'FOUND']),
    title: z.string().min(3, 'Title must be at least 3 characters').max(100, 'Title cannot exceed 100 characters'),
    description: z.string().optional(),
    category: z.enum([
      'WATCH',
      'ID_CARD',
      'BOTTLE',
      'BAG',
      'PHONE',
      'BOOKS',
      'ELECTRONICS',
      'OTHER',
    ]),
    blockId: z.number().optional(),
    roomId: z.number().optional(),
    eventTime: z
      .string()
      .min(1, 'Date and time are required')
      .refine((val) => new Date(val) <= new Date(), 'Date and time cannot be in the future'),
    imageUrl: z.string().optional(),
    handedToOffice: z.boolean(),
    verificationQuestion: z.string().optional(),
  })
  .superRefine((data, ctx) => {
    if (data.type === 'FOUND' && !data.handedToOffice && (!data.verificationQuestion || data.verificationQuestion.trim().length < 5)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['verificationQuestion'],
        message: 'Verification question is required (min 5 chars) unless handed to security/office',
      });
    }
  });

type PostItemFormValues = z.infer<typeof postItemSchema>;

export const PostItemPage: React.FC = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Wizard state: Step 1 = Type selection, Step 2 = Details form
  const [step, setStep] = useState<1 | 2>(1);
  const [selectedType, setSelectedType] = useState<ItemType>('LOST');

  // Media & AI state
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [isUploadingImage, setIsUploadingImage] = useState<boolean>(false);
  const [isAiAnalyzing, setIsAiAnalyzing] = useState<boolean>(false);
  const [aiWarning, setAiWarning] = useState<string | null>(null);
  const [tagsList, setTagsList] = useState<string[]>([]);

  // Fetch reference data
  const { data: blocks = [] } = useQuery({
    queryKey: ['referenceBlocks'],
    queryFn: referenceApi.getBlocks,
    staleTime: 1000 * 60 * 10,
  });

  const { data: categories = [] } = useQuery({
    queryKey: ['referenceCategories'],
    queryFn: referenceApi.getCategories,
    staleTime: 1000 * 60 * 10,
  });

  // Current local datetime string for input default
  const now = new Date();
  const localDatetime = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<PostItemFormValues>({
    resolver: zodResolver(postItemSchema),
    defaultValues: {
      type: 'LOST',
      title: '',
      description: '',
      category: 'OTHER',
      eventTime: localDatetime,
      handedToOffice: false,
      verificationQuestion: '',
    },
  });

  const watchedBlockId = watch('blockId');
  const watchedHandedToOffice = watch('handedToOffice');
  const selectedBlock = blocks.find((b) => b.id === Number(watchedBlockId));
  const rooms = selectedBlock?.rooms || [];

  const handleChooseType = (type: ItemType) => {
    setSelectedType(type);
    setValue('type', type);
    setStep(2);
  };

  // Image Upload and AI Analysis
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      toast.error('Image size exceeds maximum allowed limit of 5 MB');
      return;
    }

    const previewUrl = URL.createObjectURL(file);
    setImagePreview(previewUrl);

    setIsUploadingImage(true);
    setIsAiAnalyzing(true);
    setAiWarning(null);

    try {
      // 1. Upload photo to backend
      const uploadedUrl = await uploadsApi.uploadImage(file);
      setValue('imageUrl', uploadedUrl);

      // 2. Analyze photo with Gemini Vision AI
      try {
        const analysis = await aiApi.analyzeImage(file);
        if (analysis.title) setValue('title', analysis.title);
        if (analysis.category) setValue('category', analysis.category);
        if (analysis.shortDescription) setValue('description', analysis.shortDescription);
        if (analysis.tags?.length) setTagsList(analysis.tags);

        if (analysis.warningMessage) {
          setAiWarning(analysis.warningMessage);
          toast.warning('Sensitive content detected in image', {
            description: analysis.warningMessage,
          });
        } else {
          toast.success('AI auto-filled item details from your photo!', {
            description: 'Feel free to review and adjust any field below.',
          });
        }
      } catch (aiErr: unknown) {
        console.warn('AI analysis unavailable:', aiErr);
        toast.info('AI assistant is temporarily unavailable. Please fill in item details manually.');
      }
    } catch (uploadErr: unknown) {
      const msg = uploadErr instanceof Error ? uploadErr.message : 'Failed to upload photo';
      toast.error(msg);
      setImagePreview(null);
    } finally {
      setIsUploadingImage(false);
      setIsAiAnalyzing(false);
    }
  };

  const removePhoto = () => {
    setImagePreview(null);
    setValue('imageUrl', undefined);
    setAiWarning(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const onSubmit = async (values: PostItemFormValues) => {
    try {
      // Format ISO datetime
      const isoTime = new Date(values.eventTime).toISOString();

      const created = await itemsApi.createItem({
        type: values.type,
        title: values.title.trim(),
        description: values.description?.trim() || undefined,
        category: values.category,
        blockId: values.blockId ? Number(values.blockId) : undefined,
        roomId: values.roomId ? Number(values.roomId) : undefined,
        eventTime: isoTime,
        imageUrl: values.imageUrl,
        handedToOffice: values.type === 'FOUND' ? values.handedToOffice : false,
        verificationQuestion:
          values.type === 'FOUND' && !values.handedToOffice
            ? values.verificationQuestion?.trim()
            : undefined,
      });

      toast.success('Item posted successfully!');
      navigate(`/items/${created.id}`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to post item';
      toast.error(msg);
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 py-6 sm:py-10">
      {/* Wizard Step 1: Type Selection */}
      {step === 1 && (
        <div className="animate-in fade-in duration-200">
          <div className="text-center mb-8">
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-slate-900 dark:text-white">
              What would you like to report?
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1.5">
              Select whether you misplaced an item or found something on campus
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            {/* Lost Card */}
            <button
              onClick={() => handleChooseType('LOST')}
              className="text-left group p-6 rounded-2xl bg-white dark:bg-slate-900 border-2 border-slate-200 dark:border-slate-800 hover:border-rose-500 dark:hover:border-rose-500 shadow-soft hover:shadow-elevated transition-all flex flex-col justify-between"
            >
              <div>
                <div className="w-12 h-12 rounded-xl bg-rose-50 dark:bg-rose-950/60 text-rose-600 dark:text-rose-400 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                  <Search className="w-6 h-6" />
                </div>
                <h2 className="text-lg font-bold text-slate-900 dark:text-white mb-1">
                  I lost something
                </h2>
                <p className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
                  Post details of your lost belonging. We will instantly scan found items and notify you upon match.
                </p>
              </div>
              <div className="mt-6 flex items-center gap-1.5 text-xs font-semibold text-rose-600 dark:text-rose-400">
                <span>Report lost item</span>
                <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
              </div>
            </button>

            {/* Found Card */}
            <button
              onClick={() => handleChooseType('FOUND')}
              className="text-left group p-6 rounded-2xl bg-white dark:bg-slate-900 border-2 border-slate-200 dark:border-slate-800 hover:border-emerald-500 dark:hover:border-emerald-500 shadow-soft hover:shadow-elevated transition-all flex flex-col justify-between"
            >
              <div>
                <div className="w-12 h-12 rounded-xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                  <CheckCircle className="w-6 h-6" />
                </div>
                <h2 className="text-lg font-bold text-slate-900 dark:text-white mb-1">
                  I found something
                </h2>
                <p className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
                  Help return an item to a fellow student. Connect privately through verified claim questions.
                </p>
              </div>
              <div className="mt-6 flex items-center gap-1.5 text-xs font-semibold text-emerald-600 dark:text-emerald-400">
                <span>Report found item</span>
                <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
              </div>
            </button>
          </div>
        </div>
      )}

      {/* Wizard Step 2: Full Post Form */}
      {step === 2 && (
        <div className="animate-in fade-in duration-200">
          <button
            onClick={() => setStep(1)}
            className="inline-flex items-center gap-1.5 text-xs font-medium text-slate-500 hover:text-slate-800 dark:text-slate-400 dark:hover:text-slate-200 mb-4 transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" /> Back to item type selection
          </button>

          <div className="mb-6">
            <div className="flex items-center gap-2 mb-1">
              <span
                className={`text-xs font-bold px-2.5 py-0.5 rounded-full ${
                  selectedType === 'LOST'
                    ? 'bg-rose-100 dark:bg-rose-950 text-rose-700 dark:text-rose-300'
                    : 'bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300'
                }`}
              >
                {selectedType} ITEM
              </span>
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
              {selectedType === 'LOST' ? 'Report Lost Belonging' : 'Report Found Belonging'}
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
              Upload a photo to auto-fill details, or complete the fields manually
            </p>
          </div>

          <Card className="p-6 sm:p-8">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              {/* Photo Upload & Preview with AI Assist */}
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  Item Photo (Recommended)
                </label>

                {imagePreview ? (
                  <div className="relative rounded-2xl overflow-hidden border border-slate-200 dark:border-slate-800 bg-slate-900 max-h-72 flex items-center justify-center group">
                    <img
                      src={imagePreview}
                      alt="Upload preview"
                      className="max-h-72 w-full object-contain"
                    />

                    {/* AI Loading Shimmer Overlay */}
                    {isAiAnalyzing && (
                      <div className="absolute inset-0 bg-slate-900/70 backdrop-blur-xs flex flex-col items-center justify-center text-white p-4">
                        <Sparkles className="w-8 h-8 text-amber-400 animate-spin mb-2" />
                        <span className="text-sm font-semibold">Gemini AI is analyzing item...</span>
                        <span className="text-xs text-slate-300 mt-1">
                          Auto-detecting category, colors, and security checks
                        </span>
                      </div>
                    )}

                    <button
                      type="button"
                      onClick={removePhoto}
                      disabled={isUploadingImage || isAiAnalyzing}
                      className="absolute top-3 right-3 p-1.5 rounded-full bg-slate-900/80 hover:bg-rose-600 text-white transition-colors"
                      aria-label="Remove photo"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ) : (
                  <div
                    onClick={() => fileInputRef.current?.click()}
                    className="border-2 border-dashed border-slate-300 dark:border-slate-700 hover:border-brand-500 dark:hover:border-brand-500 rounded-2xl p-6 sm:p-8 text-center cursor-pointer transition-colors bg-slate-50/50 dark:bg-slate-900/30 group"
                  >
                    <div className="w-12 h-12 rounded-xl bg-brand-50 dark:bg-brand-950/60 text-brand-600 dark:text-brand-400 mx-auto flex items-center justify-center mb-3 group-hover:scale-105 transition-transform">
                      <UploadCloud className="w-6 h-6" />
                    </div>
                    <p className="text-sm font-medium text-slate-700 dark:text-slate-200">
                      Click or drag photo here to upload
                    </p>
                    <p className="text-xs text-slate-400 mt-1">
                      JPEG, PNG or WebP up to 5 MB. Enables AI auto-fill and photo matching.
                    </p>
                  </div>
                )}

                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  className="hidden"
                  onChange={handleFileChange}
                />

                {/* Sensitive Content Warning Banner */}
                {aiWarning && (
                  <div className="mt-3 p-3.5 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900 text-amber-800 dark:text-amber-200 flex items-start gap-2.5 text-xs">
                    <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-semibold block mb-0.5">Privacy Notice</span>
                      <span>{aiWarning}</span>
                    </div>
                  </div>
                )}

                {/* AI Tags display */}
                {tagsList.length > 0 && (
                  <div className="mt-2.5 flex flex-wrap gap-1.5 items-center">
                    <span className="text-[11px] text-slate-400 mr-1 flex items-center gap-1">
                      <Sparkles className="w-3 h-3 text-brand-500" /> AI Tags:
                    </span>
                    {tagsList.map((tag) => (
                      <span
                        key={tag}
                        className="text-[11px] font-medium px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300"
                      >
                        #{tag}
                      </span>
                    ))}
                  </div>
                )}
              </div>

              {/* Title & Category */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="sm:col-span-2">
                  <Input
                    label="Item Title"
                    placeholder="e.g. Titan Black Chronograph Watch"
                    error={errors.title?.message}
                    {...register('title')}
                  />
                </div>

                <div>
                  <Select
                    label="Category"
                    options={categories.map((c) => ({ value: c.name, label: c.displayName }))}
                    error={errors.category?.message}
                    {...register('category')}
                  />
                </div>
              </div>

              {/* Description */}
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
                  Detailed Description (Optional)
                </label>
                <textarea
                  rows={3}
                  placeholder="Provide recognizable details such as color, brand, scratches, or accessories..."
                  className="w-full rounded-input border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 px-3.5 py-2.5 text-sm text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
                  {...register('description')}
                />
              </div>

              {/* Location (Block & Room) */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Select
                  label="Campus Block"
                  placeholder="Select block..."
                  options={blocks.map((b) => ({ value: b.id, label: b.name }))}
                  error={errors.blockId?.message}
                  {...register('blockId', {
                    setValueAs: (v) => (v === '' ? undefined : Number(v)),
                  })}
                />

                <Select
                  label="Classroom / Hall"
                  placeholder={watchedBlockId ? 'Select room...' : 'Select block first'}
                  disabled={!watchedBlockId}
                  options={rooms.map((r) => ({ value: r.id, label: r.name }))}
                  error={errors.roomId?.message}
                  {...register('roomId', {
                    setValueAs: (v) => (v === '' ? undefined : Number(v)),
                  })}
                />
              </div>

              {/* Date and Time */}
              <Input
                label="Approximate Date & Time"
                type="datetime-local"
                error={errors.eventTime?.message}
                helperText="When the item was lost or discovered"
                {...register('eventTime')}
              />

              {/* FOUND-specific: Handed to office toggle & Verification question */}
              {selectedType === 'FOUND' && (
                <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800 space-y-4">
                  {/* Office Drop-off Toggle */}
                  <label className="flex items-start gap-3 cursor-pointer">
                    <input
                      type="checkbox"
                      className="mt-1 h-4 w-4 rounded text-brand-600 focus:ring-brand-500 border-slate-300 dark:border-slate-700"
                      {...register('handedToOffice')}
                    />
                    <div>
                      <span className="text-sm font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-1.5">
                        <Building className="w-4 h-4 text-brand-600" />
                        Handed to campus security office or library desk
                      </span>
                      <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                        Enable this if you left the physical item with staff. Students will be instructed to pick it up directly from the office.
                      </p>
                    </div>
                  </label>

                  {/* Verification Question if NOT handed to office */}
                  {!watchedHandedToOffice && (
                    <div className="pt-2 border-t border-slate-200 dark:border-slate-700">
                      <Input
                        label="Verification Question (Required for true owner verification)"
                        placeholder="e.g. What is the lock screen wallpaper or sticker on the back?"
                        error={errors.verificationQuestion?.message}
                        helperText="The claimant must answer this question correctly before you approve their claim."
                        {...register('verificationQuestion')}
                      />
                      <div className="mt-2 p-3 rounded-lg bg-amber-50 dark:bg-amber-950/40 text-amber-800 dark:text-amber-200 text-xs flex items-start gap-2">
                        <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                        <span>
                          <strong>Security Tip:</strong> Never include the answer in your description or photo!
                        </span>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* Submit Button */}
              <div className="pt-4">
                <Button
                  type="submit"
                  variant="primary"
                  isLoading={isSubmitting || isUploadingImage}
                  className="w-full text-base font-semibold"
                >
                  Post {selectedType === 'LOST' ? 'Lost' : 'Found'} Item
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  );
};
