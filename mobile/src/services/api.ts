import { API_URL } from "@/constants/config"
import axios from "axios"
import { tokenStorage } from "./tokenStorage"

const api = axios.create({
	baseURL: API_URL,
	headers: {
		"Content-Type": "application/json"
	},
	timeout: 10000
})

api.interceptors.request.use(async (config) => {
	const isAuthEndpoint =
		config.url?.includes("/api/auth/login") || config.url?.includes("/api/auth/register") || config.url?.includes("/api/auth/refresh")

	if (isAuthEndpoint) {
		return config
	}

	const token = await tokenStorage.getAccessToken()

	if (token) {
		config.headers.Authorization = `Bearer ${token}`
	}

	return config
})

api.interceptors.response.use(
	(response) => response,
	(error) => {
		console.log("API URL:", error.config?.baseURL + error.config?.url)
		console.log("Status:", error.response?.status)
		console.log("Response:", error.response?.data)

		return Promise.reject(error)
	}
)

export default api
