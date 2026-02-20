async function reviewCode() {
    const code = document.getElementById('code').value.trim();
    const language = document.getElementById('language').value;
    const provider = document.getElementById('provider').value;
    const businessRequirements = document.getElementById('businessRequirements').value.trim();
    const resultsDiv = document.getElementById('results');
    const reviewBtn = document.getElementById('reviewBtn');

    if (!code) {
        alert('Please enter some code to review!');
        return;
    }

    reviewBtn.disabled = true;
    reviewBtn.textContent = 'Reviewing...';

    resultsDiv.innerHTML = `
        <div class="loading">
            <div class="spinner"></div>
            <p>AI is analyzing your code...</p>
            <p style="font-size: 14px; color: #999; margin-top: 10px;">
                ${businessRequirements ? 'Validating business requirements...' : 'This may take a few seconds'}
            </p>
        </div>
    `;

    try {
        const requestBody = {
            code,
            language
        };

        if (businessRequirements) {
            requestBody.businessRequirements = businessRequirements;
        }

        const response = await fetch('/prompts/analyze-code', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                 'ai-provider': provider
            },
            body: JSON.stringify(requestBody)
        });

        if (!response.ok) {
            throw new Error('HTTP error ' + response.status);
        }

        const reviewText = await response.text();
        displayReview(reviewText, language, code.length, Boolean(businessRequirements));
    } catch (error) {
        console.error('Error:', error);
        showError('Failed to connect to server. Make sure the backend is running!');
    } finally {
        reviewBtn.disabled = false;
        reviewBtn.textContent = 'Review Code';
    }
}

function displayReview(review, language, codeLength, hasBusinessRequirements) {
    const resultsDiv = document.getElementById('results');

    const formattedReview = `
        <div class="review-content">
            ${formatReviewSections(review)}
        </div>

        <div class="stats">
            <span class="stat-badge">${language}</span>
            <span class="stat-badge">openai</span>
            <span class="stat-badge">${codeLength} characters</span>
            ${hasBusinessRequirements
                ? '<span class="stat-badge" style="background: #e7f5ff; color: #1971c2;">Business requirements checked</span>'
                : ''}
        </div>
    `;

    resultsDiv.innerHTML = formattedReview;
}

function formatReviewSections(review) {
    let formatted = review;

    formatted = formatted.replace(
        /\*\*BUSINESS LOGIC VALIDATION:\*\*/g,
        '<div class="review-section" style="border-left-color: #7950f2; background: #f3f0ff;"><h3>BUSINESS LOGIC VALIDATION</h3>'
    );
    formatted = formatted.replace(
        /\*\*BUGS & LOGICAL ERRORS:\*\*/g,
        '</div><div class="review-section bugs"><h3>BUGS & LOGICAL ERRORS</h3>'
    );
    formatted = formatted.replace(
        /\*\*[^\w\n]*\s*PERFORMANCE ISSUES:\*\*/g,
        '</div><div class="review-section performance"><h3>PERFORMANCE ISSUES</h3>'
    );
    formatted = formatted.replace(
        /\*\*SECURITY CONCERNS:\*\*/g,
        '</div><div class="review-section security"><h3>SECURITY CONCERNS</h3>'
    );
    formatted = formatted.replace(
        /\*\*BEST PRACTICES:\*\*/g,
        '</div><div class="review-section best-practices"><h3>BEST PRACTICES</h3>'
    );
    formatted = formatted.replace(
        /\*\*SUGGESTIONS:\*\*/g,
        '</div><div class="review-section suggestions"><h3>SUGGESTIONS</h3>'
    );
    formatted = formatted.replace(
        /\*\*OVERALL RATING:\*\*/g,
        '</div><div class="review-section rating"><h3>OVERALL RATING</h3>'
    );

    formatted += '</div>';
    formatted = formatted.replace(/\n/g, '<br>');

    return formatted;
}

function showError(message) {
    const resultsDiv = document.getElementById('results');
    resultsDiv.innerHTML = `
        <div class="error-message">
            <strong>Error:</strong> ${message}
        </div>
    `;
}

function clearCode() {
    document.getElementById('code').value = '';
    document.getElementById('businessRequirements').value = '';
    document.getElementById('results').innerHTML = `
        <div class="empty-state">
            <p>Submit your code to get AI-powered review</p>
        </div>
    `;
}

async function loadCodePlaceholder(codeInput) {
    try {
        const response = await fetch('../code-review-placeholder.txt');
        if (!response.ok) {
            throw new Error('HTTP error ' + response.status);
        }

        const placeholderText = (await response.text()).trim();
        if (!placeholderText) {
            return;
        }

        codeInput.placeholder = placeholderText;
        if (!codeInput.value.trim()) {
            codeInput.value = placeholderText;
        }
    } catch (error) {
        console.error('Failed to load code placeholder:', error);
    }
}

function initializeCodeReviewPage() {
    const codeInput = document.getElementById('code');
    const reviewBtn = document.getElementById('reviewBtn');
    const clearBtn = document.getElementById('clearBtn');

    if (!codeInput || !reviewBtn || !clearBtn) {
        return;
    }

    reviewBtn.addEventListener('click', reviewCode);
    clearBtn.addEventListener('click', clearCode);
    loadCodePlaceholder(codeInput);

    codeInput.addEventListener('keydown', function onCodeKeydown(event) {
        if (event.ctrlKey && event.key === 'Enter') {
            reviewCode();
        }
    });
}

document.addEventListener('DOMContentLoaded', initializeCodeReviewPage);
