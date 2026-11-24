export interface PublicationReceipt {
  labId: number
  status: 'published'
  publishedAt: string
  publishedBy: string
  submissionCount: number
}

export interface PublishApiResponse {
  receipt: PublicationReceipt
  replayed: boolean
}
