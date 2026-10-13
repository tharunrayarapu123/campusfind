import { useEffect, useRef, useState, useCallback } from 'react';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getStoredToken } from '../api/client';
import { chatApi } from '../api/chat';
import type { ChatMessageDto, SendChatMessagePayload } from '../types';
import { toast } from 'sonner';

export type ChatConnectionState = 'CONNECTING' | 'CONNECTED' | 'DISCONNECTED' | 'ERROR';

interface UseMatchChatProps {
  matchId: number;
  onNewMessage?: (msg: ChatMessageDto) => void;
}

export function useMatchChat({ matchId, onNewMessage }: UseMatchChatProps) {
  const [connectionState, setConnectionState] = useState<ChatConnectionState>('CONNECTING');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const clientRef = useRef<Client | null>(null);

  // Rate limiting: max 30 messages per minute
  const sentTimestampsRef = useRef<number[]>([]);

  const checkRateLimit = useCallback((): boolean => {
    const now = Date.now();
    // Keep timestamps from the last 60 seconds
    sentTimestampsRef.current = sentTimestampsRef.current.filter((ts) => now - ts < 60000);
    if (sentTimestampsRef.current.length >= 30) {
      const oldest = sentTimestampsRef.current[0];
      const waitSeconds = Math.ceil((60000 - (now - oldest)) / 1000);
      toast.error(`Rate limit exceeded (max 30 msgs/min). Please wait ${waitSeconds}s.`);
      return false;
    }
    sentTimestampsRef.current.push(now);
    return true;
  }, []);

  useEffect(() => {
    const token = getStoredToken();
    if (!token) {
      setConnectionState('ERROR');
      setErrorMessage('Authentication token missing');
      return;
    }

    setConnectionState('CONNECTING');
    setErrorMessage(null);

    // Backend /ws URL
    const wsUrl = import.meta.env.VITE_API_URL ? `${import.meta.env.VITE_API_URL}/ws` : '/ws';

    const client = new Client({
      webSocketFactory: () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const Sock = typeof SockJS === 'function' ? SockJS : (SockJS as any)?.default;
        return new Sock(wsUrl);
      },
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnectionState('CONNECTED');
        setErrorMessage(null);

        // Subscribe to topic for this match
        client.subscribe(`/topic/match/${matchId}`, (message: IMessage) => {
          try {
            const parsed: ChatMessageDto = JSON.parse(message.body);
            if (onNewMessage) {
              onNewMessage(parsed);
            }
          } catch (err) {
            console.error('Failed to parse incoming WebSocket message', err);
          }
        });
      },
      onDisconnect: () => {
        setConnectionState('DISCONNECTED');
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        setConnectionState('ERROR');
        setErrorMessage(frame.headers['message'] || 'Chat broker connection error');
      },
      onWebSocketError: (event) => {
        console.error('WebSocket connection error:', event);
        setConnectionState('DISCONNECTED');
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
      }
    };
  }, [matchId, onNewMessage]);

  const sendMessage = useCallback(
    async (payload: SendChatMessagePayload): Promise<boolean> => {
      if (!checkRateLimit()) {
        return false;
      }

      const client = clientRef.current;
      if (client && client.connected) {
        try {
          client.publish({
            destination: `/app/chat/${matchId}`,
            body: JSON.stringify(payload),
          });
          return true;
        } catch (err) {
          console.error('WebSocket publish failed, falling back to REST', err);
        }
      }

      // Fallback to REST endpoint
      try {
        const sent = await chatApi.sendMessageRest(matchId, payload);
        if (onNewMessage) {
          onNewMessage(sent);
        }
        return true;
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to send message';
        toast.error(msg);
        return false;
      }
    },
    [matchId, checkRateLimit, onNewMessage]
  );

  return {
    connectionState,
    errorMessage,
    sendMessage,
    isConnected: connectionState === 'CONNECTED',
  };
}
