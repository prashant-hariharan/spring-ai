let abortController = null;

async function startStreaming() {

    const message = document.getElementById('message').value;
    const provider = document.getElementById('provider').value;
    const conversationIdInput = document.getElementById('conversationId');
    const responseDiv = document.getElementById('response');
    const button = document.getElementById('streamBtn');

    let conversationId = conversationIdInput.value.trim();

    if (!message.trim()) {
        alert('Please enter a message!');
        return;
    }

    if (abortController) {
        abortController.abort();
    }

    responseDiv.innerHTML = '<span class="typing-indicator"></span>';
    button.disabled = true;
    button.textContent = 'Streaming...';

    abortController = new AbortController();
    let fullResponse = '';

    try {

        let url = '/chatmodel/streaming/chat';
        if (conversationId) {
            url += `/conversation?conversationId=${conversationId}`;
        }

        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain',
                'ai-provider': provider
            },
            body: message,
            signal: abortController.signal
        });

        if (!response.ok || !response.body) {
            throw new Error('Failed to connect to streaming endpoint');
        }

        const returnedConversationId = response.headers.get('conversation-id');
        if (returnedConversationId) {
            conversationIdInput.value = returnedConversationId;
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {

            const { value, done } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });

            const lines = buffer.split('\n');
            buffer = lines.pop();

            for (const line of lines) {
                if (line.startsWith('data:')) {
                    const data = line.slice(5);
                    fullResponse += data;
                    responseDiv.textContent = fullResponse;
                    responseDiv.scrollTop = responseDiv.scrollHeight;
                }
            }
        }

    } catch (error) {
        console.error('Streaming error:', error);
        if (fullResponse === '') {
            responseDiv.textContent =
                'Error: Failed to connect to streaming endpoint.';
        }
    } finally {
        button.disabled = false;
        button.textContent = 'Start Streaming';
    }
}
