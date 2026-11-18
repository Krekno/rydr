import Constants from "expo-constants"

function resolveApiUrl(): string {
	const hostUri: string | undefined = Constants.expoConfig?.hostUri
	if (!hostUri) {
		return "https://gateway-service-40oe.onrender.com"
	}

	const host = hostUri.split(":")[0]
	return host ? `${host}:8080` : "https://gateway-service-40oe.onrender.com"
}

export const API_URL = resolveApiUrl()
