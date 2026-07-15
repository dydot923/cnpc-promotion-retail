import { apiRequest } from "./request";
import type {
  MemberCreateRequest,
  MemberIdentifyRequest,
  MemberResponse,
  MemberUpdateRequest,
  PointsChangeRequest,
  PointsChangeResponse
} from "../types";

export function identifyMember(request: MemberIdentifyRequest): Promise<MemberResponse> {
  return apiRequest<MemberResponse>("/members/identify", {
    method: "POST",
    body: JSON.stringify(request)
  });
}

export function fetchMembers(): Promise<MemberResponse[]> {
  return apiRequest<MemberResponse[]>("/members");
}

export function fetchMember(memberCode: string): Promise<MemberResponse> {
  return apiRequest<MemberResponse>(`/members/${encodeURIComponent(memberCode)}`);
}

export function createMember(request: MemberCreateRequest): Promise<MemberResponse> {
  return apiRequest<MemberResponse>("/members", {
    method: "POST",
    body: JSON.stringify(request)
  });
}

export function updateMember(memberCode: string, request: MemberUpdateRequest): Promise<MemberResponse> {
  return apiRequest<MemberResponse>(`/members/${encodeURIComponent(memberCode)}`, {
    method: "PUT",
    body: JSON.stringify(request)
  });
}

export function changeMemberPoints(
  memberCode: string,
  request: PointsChangeRequest
): Promise<PointsChangeResponse> {
  return apiRequest<PointsChangeResponse>(`/members/${encodeURIComponent(memberCode)}/points`, {
    method: "POST",
    body: JSON.stringify(request)
  });
}
