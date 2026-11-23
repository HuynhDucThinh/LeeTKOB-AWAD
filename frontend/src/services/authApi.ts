import { baseApi } from './baseApi';
import type { ApiResponse } from '../types/api.types';
import type { User } from '../types/user.types';
import type { LoginFormData, SignupFormData } from '../types/auth.types';

export const authApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    login: builder.mutation<ApiResponse<User>, LoginFormData>({
      query: (credentials) => ({
        url: '/auth/login',
        method: 'POST',
        body: credentials,
      }),
      invalidatesTags: ['Auth', 'User'],
    }),
    signup: builder.mutation<ApiResponse<User>, SignupFormData>({
      query: (userData) => ({
        url: '/auth/signup',
        method: 'POST',
        body: userData,
      }),
      invalidatesTags: ['Auth', 'User'],
    }),
    logout: builder.mutation<ApiResponse<void>, void>({
      query: () => ({
        url: '/auth/logout',
        method: 'POST',
      }),
      invalidatesTags: ['Auth', 'User'],
    }),
    getMe: builder.query<ApiResponse<User>, void>({
      query: () => '/auth/me',
      providesTags: ['User'],
    }),
  }),
});

export const {
  useLoginMutation,
  useSignupMutation,
  useLogoutMutation,
  useGetMeQuery,
} = authApi;
