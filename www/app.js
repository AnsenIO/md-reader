// MD Reader - Markdown file viewer
// Features: local file browsing, .md file association, drag-and-drop

document.addEventListener('DOMContentLoaded', async () => {
    const fileBtn = document.getElementById('file-btn');
    const browseBtn = document.getElementById('browse-btn');
    const fileInput = document.getElementById('file-input');
    const contentArea = document.getElementById('content-area');
    
    // Check for pending file from file manager intent
    await checkPendingFile();
    
    // Open file dialog
    fileBtn.addEventListener('click', () => fileInput.click());
    
    fileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) loadFile(file);
    });
    
    // Browse local files
    browseBtn.addEventListener('click', browseFiles);
    
    // Drag and drop
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
        if (file) loadFile(file);
    });
    
    function loadFile(file) {
        const reader = new FileReader();
        reader.onload = (event) => {
            renderMarkdown(event.target.result, file.name);
        };
        reader.readAsText(file);
    }
    
    async function browseFiles() {
        try {
            const { Files } = await import('@capacitor/filesystem');
            
            // List Downloads directory
            let files = [];
            try {
                const result = await Files.readdir({ path: '/downloads' });
                files = result.files || [];
            } catch (e) {
                console.warn('Could not list directory:', e);
            }
            
            // Filter for markdown/text files
            const mdFiles = files.filter(f => 
                f.name.match(/\.(md|markdown|txt)$/i)
            );
            
            if (mdFiles.length > 0) {
                showFileList(mdFiles);
            } else {
                alert('No .md/.txt files in Downloads');
                fileInput.click(); // fallback
            }
        } catch (error) {
            console.error('Browse error:', error);
            alert('Could not browse files: ' + error.message);
        }
    }
    
    function showFileList(files) {
        let html = '<div class="file-list"><h3 style="margin-bottom:12px;color:#fff">Files in Downloads</h3>';
        
        files.forEach((file, index) => {
            const icon = file.name.match(/\.(md|markdown)$/i) ? '📝' : '📄';
            const size = file.size ? formatSize(file.size) : '? KB';
            
            html += `
                <div class="file-item" onclick="window.openFile(${index})" style="cursor:pointer">
                    <span class="file-icon">${icon}</span>
                    <div class="file-info">
                        <div class="file-name">${escapeHtml(file.name)}</div>
                        <div class="file-size">${size}</div>
                    </div>
                </div>
            `;
        });
        
        html += '</div>';
        html += '<div class="empty-state"><p>Tap a file to open it</p></div>';
        
        contentArea.innerHTML = html;
    }
    
    // Exposed globally for onclick handlers
    window.openFile = async function(index) {
        try {
            const { Files } = await import('@capacitor/filesystem');
            const file = event.target.closest('.file-item');
            const fileName = file.querySelector('.file-name').textContent;
            const filePath = '/downloads/' + fileName;
            
            const result = await Files.readFile({ path: filePath, encoding: 'utf-8' });
            renderMarkdown(result.data, fileName);
        } catch (error) {
            alert('Could not open: ' + error.message);
        }
    };
    
    async function checkPendingFile() {
        // Check URL params from file manager intent
        const urlParams = new URLSearchParams(window.location.search);
        const pendingPath = urlParams.get('filePath') || urlParams.get('file') || urlParams.get('path');
        
        if (pendingPath) {
            loadFileFromUri(pendingPath);
        }
    }
    
    function loadFileFromUri(uri) {
        fetch(uri)
            .then(response => response.text())
            .then(text => {
                const filename = uri.split('/').pop() || 'Unknown';
                renderMarkdown(text, filename);
            })
            .catch(err => {
                alert('Could not open file: ' + err.message);
            });
    }
    
    function renderMarkdown(markdown, filename) {
        marked.setOptions({ breaks: true, gfm: true, headerIds: false, mangle: false });
        const html = marked.parse(markdown);
        
        contentArea.innerHTML = `
            <div class="md-content">
                <h2>${escapeHtml(filename)}</h2>
                ${html}
            </div>
        `;
        contentArea.scrollTop = 0;
    }
    
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    function formatSize(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }
});
