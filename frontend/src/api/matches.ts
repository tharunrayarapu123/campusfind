import { apiClient } from './client';
import type {
  ApiResponse,
  ClaimResponse,
  MatchResponseDto,
  PageData,
  VerificationQuestionResponse,
} from '../types';

export const matchesApi = {
  getMyMatches: async (page = 0, size = 20): Promise<PageData<MatchResponseDto>> => {
    const res = await apiClient.get<ApiResponse<PageData<MatchResponseDto>>>('/matches', {
      params: { page, size },
    });
    return res.data.data;
  },

  getMatchById: async (id: number): Promise<MatchResponseDto> => {
    const res = await apiClient.get<ApiResponse<MatchResponseDto>>(`/matches/${id}`);
    return res.data.data;
  },

  rejectMatch: async (id: number): Promise<MatchResponseDto> => {
    const res = await apiClient.post<ApiResponse<MatchResponseDto>>(`/matches/${id}/reject`);
    return res.data.data;
  },

  getVerificationQuestion: async (id: number): Promise<VerificationQuestionResponse> => {
    const res = await apiClient.get<ApiResponse<VerificationQuestionResponse>>(`/matches/${id}/verification`);
    return res.data.data;
  },

  submitClaim: async (id: number, answer: string): Promise<ClaimResponse> => {
    const res = await apiClient.post<ApiResponse<ClaimResponse>>(`/matches/${id}/claim`, { answer });
    return res.data.data;
  },

  getClaims: async (id: number): Promise<ClaimResponse[]> => {
    const res = await apiClient.get<ApiResponse<ClaimResponse[]>>(`/matches/${id}/claims`);
    return res.data.data;
  },

  approveClaim: async (matchId: number, claimId: number): Promise<ClaimResponse> => {
    const res = await apiClient.post<ApiResponse<ClaimResponse>>(`/matches/${matchId}/claims/${claimId}/approve`);
    return res.data.data;
  },

  rejectClaim: async (matchId: number, claimId: number): Promise<ClaimResponse> => {
    const res = await apiClient.post<ApiResponse<ClaimResponse>>(`/matches/${matchId}/claims/${claimId}/reject`);
    return res.data.data;
  },

  submitOfficeClaim: async (id: number, proofDescription: string): Promise<ClaimResponse> => {
    const res = await apiClient.post<ApiResponse<ClaimResponse>>(`/matches/${id}/claim-office`, {
      proofDescription,
    });
    return res.data.data;
  },

  markReturned: async (id: number): Promise<MatchResponseDto> => {
    const res = await apiClient.post<ApiResponse<MatchResponseDto>>(`/matches/${id}/returned`);
    return res.data.data;
  },
};
