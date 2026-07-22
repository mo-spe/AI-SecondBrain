const API_BASE_URL = "http://localhost:8080/api";

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.action === "apiRequest") {
        handleApiRequest(message, sendResponse);
        return true;
    } else if (message.action === "getToken") {
        handleGetToken(sendResponse);
        return true;
    }
});

async function handleApiRequest(message, sendResponse) {
    try {
        const url = API_BASE_URL + message.path;
        console.log("API Request:", url, message.method, message.body);

        const options = {
            method: message.method || "GET",
            headers: {
                "Content-Type": "application/json",
                ...(message.headers || {})
            },
            body: message.body ? JSON.stringify(message.body) : null
        };

        const response = await fetch(url, options);
        console.log("API Response status:", response.status);

        const result = await response.json();
        console.log("API Response body:", result);

        sendResponse({
            success: response.ok && result.code === 200,
            status: response.status,
            data: result
        });
    } catch (error) {
        console.error("API Request failed:", error);
        sendResponse({
            success: false,
            error: error.message
        });
    }
}

function handleGetToken(sendResponse) {
    chrome.storage.local.get(["authToken"], (result) => {
        sendResponse({
            success: true,
            token: result.authToken || null
        });
    });
}

chrome.runtime.onInstalled.addListener(() => {
    console.log("AI SecondBrain Collector installed");
});

chrome.runtime.onStartup.addListener(() => {
    console.log("AI SecondBrain Collector started");
});

chrome.runtime.onSuspend.addListener(() => {
    console.log("AI SecondBrain Collector suspended");
});
