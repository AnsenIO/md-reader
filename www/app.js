// MD Reader - Simple markdown file viewer
// Optimized for Samsung Z Fold 5 (7.6" display)

document.addEventListener('DOMContentLoaded', () => {
    const fileBtn = document.getElementById('file-btn');
    const fileInput = document.getElementById('file-input');
    const contentArea = document.getElementById('content-area');
    
    // Open file dialog
    fileBtn.addEventListener('click', () => {
        fileInput.click();
    });
    
    // Handle file selection
    fileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (!file) return;
        
        const reader = new FileReader();
        reader.onload = (event) => {
            const markdown = event.target.result;
            renderMarkdown(markdown, file.name);
        };
        reader.readAsText(file);
    });
    
    // Also support drag and drop
    contentArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        contentArea.style.background = '#1a1a2e';
    });
    
    contentArea.addEventListener('dragleave', () => {
        contentArea.style.background = '';
    });
    
    contentArea.addEventListener('drop', (e) => {
        e.preventDefault();
        contentArea.style.background = '';
        
        const file = e.dataTransfer.files[0];
        if (file && (file.name.endsWith('.md') || file.name.endsWith('.markdown') || file.name.endsWith('.txt'))) {
            const reader = new FileReader();
            reader.onload = (event) => {
                renderMarkdown(event.target.result, file.name);
            };
            reader.readAsText(file);
        }
    });
    
    function renderMarkdown(markdown, filename) {
        // Configure marked
        marked.setOptions({
            breaks: true,
            gfm: true,
            headerIds: false,
            mangle: false
        });
        
        // Render markdown to HTML
        const html = marked.parse(markdown);
        
        // Display content
        contentArea.innerHTML = `
            <div class="md-content">
                <h2>${escapeHtml(filename)}</h2>
                ${html}
            </div>
        `;
    }
    
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
});
