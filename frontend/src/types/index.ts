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

export interface MatchResponseDto {
  id: number;
  lostItem: ItemSummary;
  foundItem: ItemSummary;
  score: number;
  scoreBreakdown: MatchScoreBreakdown;
  status: MatchStatus;
  otherUser: PublicUser;
  createdAt: string;
}

export interface VerificationQuestionResponse {
  matchId: number;
  verificationQuestion: string;
  handedToOffice: boolean;
  instructions?: string;
}

export interface ClaimResponse {
  id: number;
  matchId: number;
  claimant: PublicUser;
  answer: string;
  approved: boolean | null;
  officeClaim: boolean;
  createdAt: string;
}

export interface ClaimRequest {
  answer: string;
}

export interface OfficeClaimRequest {
  proofDescription: string;
}

export interface ChatMessageDto {
  id: number;
  matchId: number;
  sender: PublicUser;
  body: string;
  imageUrl?: string;
  createdAt: string;
}

export interface SendChatMessagePayload {
  body: string;
  imageUrl?: string;
}

export interface NotificationDto {
  id: number;
  title: string;
  body: string;
  link?: string;
  read: boolean;
  createdAt: string;
}

export interface UnreadCountDto {
  unreadCount: number;
}

export interface LeaderboardUserDto {
  id: number;
  fullName: string;
  department?: string;
  year?: number;
  avatarUrl?: string;
  karmaPoints: number;
  badges: string[];
  returnsCount: number;
}

export interface UserStatsDto {
  postsCount: number;
  returnsCount: number;
  karmaPoints: number;
  badges: string[];
}

export interface UpdateUserRequest {
  fullName: string;
  department?: string;
  year?: number;
  phone?: string;
  avatarUrl?: string;
}

export interface ChangePasswordRequest {
  oldPassword?: string;
  currentPassword?: string;
  newPassword?: string;
}

export interface UpdateSettingsRequest {
  emailNotifications: boolean;
}

export interface AdminStatsDto {
  status: string;
  totalUsers: number;
  totalLost: number;
  totalFound: number;
  returned: number;
  returnRatePercentage: number;
  openItems: number;
  matchesCount: number;
  itemsPerBlock: Record<string, number>;
}

export interface ReportDto {
  id: number;
  itemId: number;
  itemTitle: string;
  reporter: PublicUser;
  reason: string;
  createdAt: string;
}

export interface AuditLogDto {
  id: number;
  adminId: number;
  adminEmail: string;
  action: string;
  targetType: string;
  targetId: number;
  details: string;
  createdAt: string;
}

