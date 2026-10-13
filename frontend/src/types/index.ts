export type Role = 'STUDENT' | 'ADMIN';

export type ItemType = 'LOST' | 'FOUND';

export type ItemCategory =
  | 'WATCH'
  | 'ID_CARD'
  | 'BOTTLE'
  | 'BAG'
  | 'PHONE'
  | 'BOOKS'
  | 'ELECTRONICS'
  | 'OTHER';

export type ItemStatus = 'OPEN' | 'MATCHED' | 'RETURNED' | 'CLOSED';

export type MatchStatus = 'SUGGESTED' | 'CONFIRMED' | 'REJECTED' | 'RETURNED';

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
}

export interface HealthCheckResponse {
  status: string;
  service: string;
  activeProfile?: string;
  databaseStatus?: string;
  serverTime?: string;
}

export interface UserProfile {
  id: number;
  fullName: string;
  email: string;
  department?: string;
  year?: number;
  phone?: string;
  avatarUrl?: string;
  karmaPoints: number;
  role: Role;
  emailNotifications?: boolean;
}

export interface PublicUser {
  id: number;
  fullName: string;
  department?: string;
  year?: number;
  avatarUrl?: string;
  karmaPoints: number;
}

export interface AuthResponse {
  token: string;
  user: UserProfile;
}

export interface Room {
  id: number;
  name: string;
}

export interface Block {
  id: number;
  name: string;
  rooms: Room[];
}

export interface CategoryInfo {
  name: ItemCategory;
  displayName: string;
}

export interface ItemSummary {
  id: number;
  type: ItemType;
  title: string;
  description?: string;
  category: ItemCategory;
  blockName?: string;
  roomName?: string;
  eventTime: string;
  imageUrl?: string;
  status: ItemStatus;
  handedToOffice: boolean;
  owner: PublicUser;
  createdAt: string;
}

export interface ItemDetail {
  id: number;
  type: ItemType;
  title: string;
  description?: string;
  category: ItemCategory;
  blockId?: number;
  blockName?: string;
  roomId?: number;
  roomName?: string;
  eventTime: string;
  imageUrl?: string;
  latitude?: number;
  longitude?: number;
  handedToOffice: boolean;
  verificationQuestion?: string;
  status: ItemStatus;
  hidden: boolean;
  owner: PublicUser;
  createdAt: string;
  updatedAt?: string;
}

export interface SensitiveContent {
  hasFace: boolean;
  hasIdNumber: boolean;
  hasOtherPersonalInfo: boolean;
}

export interface ImageAnalysisResponse {
  category: ItemCategory;
  title: string;
  color?: string;
  brand?: string;
  tags: string[];
  shortDescription?: string;
  sensitiveContent: SensitiveContent;
  warningMessage?: string;
}

export interface SimilarItem {
  item: ItemSummary;
  similarityScore: number;
  matchReason: string;
}

export interface MatchScoreBreakdown {
  block?: number;
  room?: number;
  category?: number;
  time?: number;
  text?: number;
  image?: number;
  total?: number;
  imageCompared?: boolean;
  rawSimilarity?: number;
  reason?: string;
  [key: string]: unknown;
}

export interface MatchSummary {
  id: number;
  lostItemId: number;
  lostItemTitle: string;
  lostItemImageUrl?: string;
  foundItemId: number;
  foundItemTitle: string;
  foundItemImageUrl?: string;
  score: number;
  scoreBreakdown: MatchScoreBreakdown;
  status: MatchStatus;
  otherUser: PublicUser;
  isLostItemOwner: boolean;
  handedToOffice: boolean;
  verificationQuestion?: string;
  hasClaimed: boolean;
  chatUnlocked: boolean;
  createdAt: string;
}

export interface PageData<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface CreateItemPayload {
  type: ItemType;
  title: string;
  description?: string;
  category: ItemCategory;
  blockId?: number;
  roomId?: number;
  eventTime: string;
  imageUrl?: string;
  latitude?: number;
  longitude?: number;
  handedToOffice?: boolean;
  verificationQuestion?: string;
}

export interface UpdateItemPayload {
  title?: string;
  description?: string;
  category?: ItemCategory;
  blockId?: number;
  roomId?: number;
  eventTime?: string;
  imageUrl?: string;
  latitude?: number;
  longitude?: number;
  verificationQuestion?: string;
}
