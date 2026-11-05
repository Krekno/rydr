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
	const token = await tokenStorage.getAccessToken()

	if (token) {
		config.headers.Authorization = `Bearer ${token}`
	}

	return config
})

export default api
