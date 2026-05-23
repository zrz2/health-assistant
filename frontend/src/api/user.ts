import request from './request'

export interface UserProfile {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatarUrl: string
  userType: number
}

export interface HealthRecord {
  age: number
  gender: number
  height: number
  weight: number
  bloodType: string
  medicalHistory: string
  allergies: string
  chronicDiseases: string
  currentMedications: string
  lifestyle: Record<string, any>
}

export function getProfile() {
  return request.get<UserProfile>('/user/profile')
}

export function updateProfile(data: Partial<UserProfile>) {
  return request.put('/user/profile', data)
}

export function getHealthRecord() {
  return request.get<HealthRecord>('/user/health-record')
}

export function updateHealthRecord(data: HealthRecord) {
  return request.put('/user/health-record', data)
}
