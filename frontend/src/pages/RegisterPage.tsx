import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { Compass, UserPlus, ShieldCheck } from 'lucide-react';
import { useAuth } from '../features/auth/AuthContext';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { Card } from '../components/ui/Card';

const registerSchema = z.object({
  fullName: z.string().min(2, 'Full name must be at least 2 characters'),
  email: z
    .string()
    .email('Please enter a valid email address')
    .refine(
      (email) => email.endsWith('@college.edu'),
      'Must be your official institutional email ending with @college.edu'
    ),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters long')
    .regex(/[A-Za-z]/, 'Password must contain at least one letter')
    .regex(/[0-9]/, 'Password must contain at least one digit'),
  department: z.string().min(1, 'Please select your department'),
  year: z.number().min(1, 'Year is required').max(5, 'Year must be between 1 and 5'),
  phone: z
    .string()
    .optional()
    .refine((val) => !val || /^[0-9+\-\s()]{7,15}$/.test(val), 'Invalid phone format'),
});

type RegisterFormValues = z.infer<typeof registerSchema>;

const DEPARTMENTS = [
  { value: 'Computer Science', label: 'Computer Science' },
  { value: 'Electronics & Comm', label: 'Electronics & Comm (ECE)' },
  { value: 'Mechanical Eng', label: 'Mechanical Engineering' },
  { value: 'Civil Eng', label: 'Civil Engineering' },
  { value: 'Information Tech', label: 'Information Technology' },
  { value: 'Electrical Eng', label: 'Electrical Engineering' },
  { value: 'Business Administration', label: 'Business Administration (MBA)' },
  { value: 'Biotechnology', label: 'Biotechnology' },
  { value: 'Other', label: 'Other' },
];

const YEARS = [
  { value: 1, label: '1st Year (Freshman)' },
  { value: 2, label: '2nd Year (Sophomore)' },
  { value: 3, label: '3rd Year (Junior)' },
  { value: 4, label: '4th Year (Senior)' },
  { value: 5, label: 'Postgraduate / Graduate' },
];

export const RegisterPage: React.FC = () => {
  const { register: registerUser } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: '',
      email: '',
      password: '',
      department: '',
      year: 1,
      phone: '',
    },
  });

  const enteredPassword = watch('password', '');
  const hasMinLength = enteredPassword.length >= 8;
  const hasLetter = /[A-Za-z]/.test(enteredPassword);
  const hasNumber = /[0-9]/.test(enteredPassword);

  const onSubmit = async (values: RegisterFormValues) => {
    setServerError(null);
    try {
      await registerUser({
        fullName: values.fullName.trim(),
        email: values.email.trim().toLowerCase(),
        password: values.password,
        department: values.department,
        year: Number(values.year),
        phone: values.phone?.trim() || undefined,
      });
      toast.success('Registration successful! Welcome to CampusFind.');
      navigate('/', { replace: true });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Registration failed';
      setServerError(msg);
      toast.error(msg);
    }
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-lg">
        <div className="text-center mb-8">
          <div className="w-12 h-12 rounded-2xl bg-brand-600 text-white mx-auto flex items-center justify-center mb-4 shadow-soft">
            <Compass className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Create Student Account
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Sign up using your verified college credentials
          </p>
        </div>

        <Card className="p-6 sm:p-8">
          {serverError && (
            <div className="mb-5 p-3.5 rounded-xl bg-rose-50 dark:bg-rose-950/50 border border-rose-200 dark:border-rose-900/60 text-xs text-rose-700 dark:text-rose-300 font-medium">
              {serverError}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input
              label="Full Name"
              placeholder="e.g. Alex Johnson"
              error={errors.fullName?.message}
              {...register('fullName')}
            />

            <Input
              label="College Email"
              type="email"
              placeholder="alex@college.edu"
              helperText="Only emails ending with @college.edu are accepted"
              error={errors.email?.message}
              {...register('email')}
            />

            <div>
              <Input
                label="Password"
                type="password"
                placeholder="••••••••"
                error={errors.password?.message}
                {...register('password')}
              />
              {/* Password strength indicators */}
              <div className="mt-2 grid grid-cols-3 gap-1.5 text-[11px]">
                <div
                  className={`p-1 rounded text-center font-medium ${
                    hasMinLength
                      ? 'bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-400'
                  }`}
                >
                  8+ Chars
                </div>
                <div
                  className={`p-1 rounded text-center font-medium ${
                    hasLetter
                      ? 'bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-400'
                  }`}
                >
                  1 Letter
                </div>
                <div
                  className={`p-1 rounded text-center font-medium ${
                    hasNumber
                      ? 'bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-400'
                  }`}
                >
                  1 Number
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Select
                label="Department"
                placeholder="Choose department..."
                options={DEPARTMENTS}
                error={errors.department?.message}
                {...register('department')}
              />

              <Select
                label="Year of Study"
                options={YEARS}
                error={errors.year?.message}
                {...register('year', { valueAsNumber: true })}
              />
            </div>

            <div>
              <Input
                label="Phone Number (Optional)"
                type="tel"
                placeholder="+1 555-0199"
                error={errors.phone?.message}
                {...register('phone')}
              />
              <div className="mt-1.5 flex items-center gap-1.5 text-[11px] text-slate-500 dark:text-slate-400">
                <ShieldCheck className="w-3.5 h-3.5 text-brand-500 shrink-0" />
                <span>Your phone number is strictly private and never shown to other students.</span>
              </div>
            </div>

            <div className="pt-2">
              <Button
                type="submit"
                variant="primary"
                isLoading={isSubmitting}
                className="w-full text-base"
              >
                <UserPlus className="w-4 h-4 mr-2" />
                Create Account
              </Button>
            </div>
          </form>

          <div className="mt-6 text-center text-xs text-slate-500 dark:text-slate-400">
            Already have an account?{' '}
            <Link
              to="/login"
              className="text-brand-600 dark:text-brand-400 font-semibold hover:underline"
            >
              Sign in
            </Link>
          </div>
        </Card>
      </div>
    </div>
  );
};
