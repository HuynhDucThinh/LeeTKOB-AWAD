import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { User, AuthState } from '../../types/user.types';
import { authApi } from '../../services/authApi';

const initialState: AuthState = {
  user: null,
  isAuthenticated: false,
  isInitialized: false,
};

export const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setUser: (state, action: PayloadAction<User | null>) => {
      state.user = action.payload;
      state.isAuthenticated = !!action.payload;
      state.isInitialized = true;
    },
    logoutAction: (state) => {
      state.user = null;
      state.isAuthenticated = false;
      state.isInitialized = true;
    },
  },
  extraReducers: (builder) => {
    builder
      .addMatcher(authApi.endpoints.login.matchFulfilled, (state, { payload }) => {
        if (payload.data) {
          state.user = payload.data;
          state.isAuthenticated = true;
          state.isInitialized = true;
        }
      })
      .addMatcher(authApi.endpoints.signup.matchFulfilled, (state, { payload }) => {
        if (payload.data) {
          state.user = payload.data;
          state.isAuthenticated = true;
          state.isInitialized = true;
        }
      })
      .addMatcher(authApi.endpoints.getMe.matchFulfilled, (state, { payload }) => {
        if (payload.data) {
          state.user = payload.data;
          state.isAuthenticated = true;
        }
        state.isInitialized = true;
      })
      .addMatcher(authApi.endpoints.getMe.matchRejected, (state) => {
        state.user = null;
        state.isAuthenticated = false;
        state.isInitialized = true;
      })
      .addMatcher(authApi.endpoints.logout.matchFulfilled, (state) => {
        state.user = null;
        state.isAuthenticated = false;
        state.isInitialized = true;
      });
  },
});

export const { setUser, logoutAction } = authSlice.actions;
export default authSlice.reducer;
