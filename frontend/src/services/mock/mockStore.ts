import { mockAppeals, mockClasses, mockLabs, mockReviewItems, mockSubmissions, mockUsers } from './mockData'

export const mockStore = {
  users: structuredClone(mockUsers),
  classes: structuredClone(mockClasses),
  labs: structuredClone(mockLabs),
  reviewItems: structuredClone(mockReviewItems),
  submissions: structuredClone(mockSubmissions),
  appeals: structuredClone(mockAppeals),
}

export const wait = (milliseconds = 180) => new Promise((resolve) => setTimeout(resolve, milliseconds))
