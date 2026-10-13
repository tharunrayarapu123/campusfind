import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Send,
  Image as ImageIcon,
  Shield,
  WifiOff,
  ChevronLeft,
  Loader2,
  Lock,
  CheckCheck,
} from 'lucide-react';
import { chatApi } from '../api/chat';
import { matchesApi } from '../api/matches';
import { apiClient } from '../api/client';
import { useAuth } from '../features/auth/AuthContext';
import { useMatchChat } from '../hooks/useMatchChat';
import type { ChatMessageDto, ApiResponse } from '../types';
import { Button } from '../components/ui/Button';
import { toast } from 'sonner';
import { cn } from '../lib/utils';

export const ChatPage: React.FC = () => {
  const { matchId: matchIdParam } = useParams<{ matchId: string }>();
  const matchId = Number(matchIdParam);
  const navigate = useNavigate();
  const { user } = useAuth();

  const [messages, setMessages] = useState<ChatMessageDto[]>([]);
  const [inputText, setInputText] = useState('');
  const [isUploadingImage, setIsUploadingImage] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 1. Fetch match to check status (if RETURNED, make read-only) and verify access
  const { data: match } = useQuery({
    queryKey: ['match', matchId],
    queryFn: () => matchesApi.getMatchById(matchId),
    enabled: !!matchId,
  });

  // 2. Fetch initial chat history
  const {
    data: historyData,
    isLoading: historyLoading,
  } = useQuery({
    queryKey: ['chatMessages', matchId],
    queryFn: () => chatApi.getMessages(matchId, 0, 50),
    enabled: !!matchId,
  });

  // Populate messages from history
  useEffect(() => {
    if (historyData?.content) {
      setMessages(historyData.content);
    }
  }, [historyData]);

  // 3. Connect to WebSocket
  const handleIncomingMessage = (newMsg: ChatMessageDto) => {
    setMessages((prev) => {
      // deduplicate
      if (prev.some((m) => m.id === newMsg.id)) {
        return prev;
      }
      return [...prev, newMsg];
    });
  };

  const { connectionState, sendMessage } = useMatchChat({
    matchId,
    onNewMessage: handleIncomingMessage,
  });

  // Scroll to bottom on new message
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSendText = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const trimmed = inputText.trim();
    if (!trimmed || isReadOnly) return;

    setInputText('');
    const success = await sendMessage({ body: trimmed });
    if (!success) {
      setInputText(trimmed);
    }
  };

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || isReadOnly) return;

    if (file.size > 5 * 1024 * 1024) {
      toast.error('Image size must be less than 5 MB');
      return;
    }

    try {
      setIsUploadingImage(true);
      const formData = new FormData();
      formData.append('file', file);

      const res = await apiClient.post<ApiResponse<{ url: string }>>('/uploads/image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      const imageUrl = res.data.data?.url;
      if (imageUrl) {
        await sendMessage({
          body: 'Shared a photo',
          imageUrl,
        });
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to upload photo';
      toast.error(msg);
    } finally {
      setIsUploadingImage(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const isReadOnly = match?.status === 'RETURNED' || match?.status === 'REJECTED';
  const otherStudentName = match?.otherUser?.fullName
    ? match.otherUser.fullName.split(' ')[0]
    : 'Matched Student';
  const otherStudentDept = match?.otherUser?.department || 'Campus Student';

  return (
    <div className="max-w-3xl mx-auto h-[calc(100vh-4rem)] md:h-[calc(100vh-5rem)] flex flex-col bg-white dark:bg-slate-900 border-x border-slate-200 dark:border-slate-800 shadow-sm">
      {/* Top Header */}
      <div className="p-3.5 sm:p-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between bg-white/90 dark:bg-slate-900/90 backdrop-blur-md sticky top-0 z-10">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(`/matches/${matchId}`)}
            className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>

          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-full bg-brand-100 dark:bg-brand-950 text-brand-700 dark:text-brand-300 font-bold flex items-center justify-center text-sm border border-brand-200 dark:border-brand-800">
              {otherStudentName.charAt(0).toUpperCase()}
            </div>
            <div>
              <div className="text-sm font-bold text-slate-900 dark:text-white leading-tight">
                {otherStudentName}
              </div>
              <div className="text-[11px] text-slate-500 dark:text-slate-400">
                {otherStudentDept} • {match ? `${match.lostItem?.title} / ${match.foundItem?.title}` : 'Match Chat'}
              </div>
            </div>
          </div>
        </div>

        <Link to={`/matches/${matchId}`}>
          <Button variant="ghost" size="sm" className="text-xs">
            Match Details
          </Button>
        </Link>
      </div>

      {/* Safety Tip Banner */}
      <div className="bg-amber-50 dark:bg-amber-950/40 border-b border-amber-200/80 dark:border-amber-900/40 px-4 py-2 flex items-center gap-2 text-xs text-amber-800 dark:text-amber-200">
        <Shield className="w-4 h-4 text-amber-600 shrink-0" />
        <span>
          <strong className="font-semibold">Safety First:</strong> Meet in a well-lit, public location on campus (e.g. Central Library, Student Center) during daytime.
        </span>
      </div>

      {/* Reconnection Banner */}
      {connectionState !== 'CONNECTED' && !isReadOnly && (
        <div className="bg-rose-500 text-white text-xs px-4 py-1.5 flex items-center justify-center gap-2 animate-pulse font-medium">
          <WifiOff className="w-3.5 h-3.5" />
          <span>
            {connectionState === 'CONNECTING'
              ? 'Connecting to realtime chat...'
              : 'Reconnecting to chat server... HTTP fallback active.'}
          </span>
        </div>
      )}

      {/* Read-Only Notice */}
      {isReadOnly && (
        <div className="bg-slate-100 dark:bg-slate-800 border-b border-slate-200 dark:border-slate-700 px-4 py-2 text-center text-xs text-slate-600 dark:text-slate-300 flex items-center justify-center gap-2">
          <Lock className="w-3.5 h-3.5 text-slate-500" />
          <span>This match is finalized ({match?.status}). Chat history is preserved as read-only.</span>
        </div>
      )}

      {/* Messages Scroll Area */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-50/50 dark:bg-slate-950/40">
        {historyLoading ? (
          <div className="flex items-center justify-center h-full">
            <Loader2 className="w-6 h-6 animate-spin text-brand-600" />
          </div>
        ) : messages.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-center p-6 text-slate-400">
            <div className="w-12 h-12 rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-center mb-2">
              <Shield className="w-6 h-6 text-brand-600" />
            </div>
            <p className="text-xs font-semibold text-slate-600 dark:text-slate-300">End-to-End Matched Chat</p>
            <p className="text-[11px] text-slate-400 max-w-xs mt-1">
              Say hello to {otherStudentName} and agree on a campus spot to return the item.
            </p>
          </div>
        ) : (
          messages.map((msg) => {
            const isMe = msg.sender?.id === user?.id;
            const senderFirst = msg.sender?.fullName
              ? msg.sender.fullName.split(' ')[0]
              : 'Student';

            return (
              <div
                key={msg.id || `${msg.createdAt}-${msg.body}`}
                className={cn('flex flex-col', isMe ? 'items-end' : 'items-start')}
              >
                <div
                  className={cn(
                    'max-w-[80%] sm:max-w-[70%] rounded-2xl p-3 shadow-xs space-y-1.5',
                    isMe
                      ? 'bg-brand-600 text-white rounded-tr-xs'
                      : 'bg-white dark:bg-slate-800 text-slate-900 dark:text-white border border-slate-200/80 dark:border-slate-700/80 rounded-tl-xs'
                  )}
                >
                  {/* Sender label for other party */}
                  {!isMe && (
                    <div className="text-[10px] font-bold text-brand-600 dark:text-brand-400">
                      {senderFirst}
                    </div>
                  )}

                  {/* Optional Image */}
                  {msg.imageUrl && (
                    <div className="rounded-xl overflow-hidden max-h-60 bg-black/5">
                      <img
                        src={msg.imageUrl}
                        alt="Shared in chat"
                        className="w-full h-auto object-cover rounded-xl"
                      />
                    </div>
                  )}

                  {/* Body Text */}
                  <p className="text-xs leading-relaxed whitespace-pre-wrap break-words">
                    {msg.body}
                  </p>

                  {/* Time + Status */}
                  <div
                    className={cn(
                      'text-[9px] flex items-center justify-end gap-1 font-medium',
                      isMe ? 'text-white/70' : 'text-slate-400'
                    )}
                  >
                    <span>{new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    {isMe && <CheckCheck className="w-3 h-3 text-white/80" />}
                  </div>
                </div>
              </div>
            );
          })
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Form Bar */}
      {!isReadOnly ? (
        <form
          onSubmit={handleSendText}
          className="p-3 bg-white dark:bg-slate-900 border-t border-slate-200 dark:border-slate-800 flex items-center gap-2"
        >
          {/* Image Upload Trigger */}
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleImageUpload}
            accept="image/jpeg,image/png,image/webp"
            className="hidden"
          />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={isUploadingImage}
            title="Attach a photo"
            className="p-2.5 rounded-xl text-slate-500 hover:text-brand-600 dark:hover:text-brand-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors disabled:opacity-50 min-h-touch min-w-touch flex items-center justify-center"
          >
            {isUploadingImage ? (
              <Loader2 className="w-5 h-5 animate-spin text-brand-600" />
            ) : (
              <ImageIcon className="w-5 h-5" />
            )}
          </button>

          {/* Text Input */}
          <input
            type="text"
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            maxLength={1000}
            placeholder="Type your message..."
            className="flex-1 text-xs sm:text-sm rounded-xl border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 px-3.5 py-2.5 text-slate-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-brand-500 transition-all"
          />

          {/* Send Button */}
          <button
            type="submit"
            disabled={!inputText.trim()}
            className="p-2.5 rounded-xl bg-brand-600 text-white hover:bg-brand-700 disabled:opacity-40 transition-colors min-h-touch min-w-touch flex items-center justify-center"
          >
            <Send className="w-4 h-4" />
          </button>
        </form>
      ) : (
        <div className="p-3 bg-slate-50 dark:bg-slate-900 border-t border-slate-200 dark:border-slate-800 text-center text-xs text-slate-500">
          Chat closed
        </div>
      )}
    </div>
  );
};

export default ChatPage;
