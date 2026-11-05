import * as SecureStore from "expo-secure-store"

export const tokenStorage = {
	async getAccessToken() {
		return SecureStore.getItemAsync("accessToken")
	},

	async setAccessToken(token: string) {
		return SecureStore.setItemAsync("accessToken", token)
	},

	async removeAccessToken() {
		return SecureStore.deleteItemAsync("accessToken")
	},

	async getRefreshToken() {
		return SecureStore.getItemAsync("refreshToken")
	},

	async setRefreshToken(token: string) {
		return SecureStore.setItemAsync("refreshToken", token)
	},

	async removeRefreshToken() {
		return SecureStore.deleteItemAsync("refreshToken")
	}
}
