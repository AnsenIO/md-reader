// MD Reader — markdown viewer for Android (Capacitor)
// Features: .md association from file managers (FileOpen plugin), Browse directory navigator,
// Open File fallback, drag-and-drop. See .scratch/md-reader-apk/ tickets 03 + 04 for decisions.

const BROWSE_ROOT = '/storage/emulated/0/Download'; // shared Downloads on consumer Android devices
const MD_EXT_RE = /\.md$/i;                          // ticket 04: md only, for now

document.addEventListener('DOMContentLoaded', async () => {
    const fileBtn = document.getElementById('file-btn');
    const browseBtn = document.getElementById('browse-btn');
    const fileInput = document.getElementById('file-input');
    const contentArea = document.getElementById('content-area');

    let fileOpenPlugin = null;
    try {
        const Capacitor = window.Capacitor;
        fileOpenPlugin = Capacitor && Capacitor.Plugins ? Capacitor.Plugins.FileOpen : null;
    } catch (e) {
        console.warn('FileOpen plugin not available:', e);
    }

    // ---- pending file from a file manager / share sheet -----------------------
    // The native side fires "filePending" retained until this listener registers,
    // so the race (intent arrives before JS exists) is covered by Capacitor itself.
    let lastOpenedUri = null;

    if (fileOpenPlugin) {
        try {
            await fileOpenPlugin.addListener('filePending', ({ uri }) => {
                console.log('FileOpen event: ' + uri);
                openUri(uri);
            });
        } catch (e) {
            console.warn('filePending listener failed:', e);
        }
    }

    // Safety net for events lost across process death / WebView reloads.
    const checkStored = async () => {
        if (!fileOpenPlugin || lastOpenedUri) return;
        try {
            const r = await fileOpenPlugin.consumePending();
            if (r && r.uri) {
                console.log('consumePending: ' + r.uri);
                openUri(r.uri);
            }
        } catch (e) {
            console.warn('consumePending failed:', e);
        }
    };

    // ---- opening files ---------------------------------------------------------
    async function openUri(uri) {
        if (!uri || uri === lastOpenedUri) return;   // dedupe: event + stored-URI can both fire
        lastOpenedUri = uri;
        try {
            let text;
            let name;
            if (/^(content|file):/i.test(uri)) {
                // content:// (SAF) and file:// are not fetchable from the WebView — native read.
                const r = await fileOpenPlugin.readFile({ path: uri, encoding: 'utf8' });
                text = r.data;
                name = r.name || fileNameFromUri(uri);
            } else {
                const res = await fetch(uri);
                if (!res.ok) throw new Error('HTTP ' + res.status);
                text = await res.text();
                name = decodeURIComponent(new URL(uri, location.href).pathname.split('/').pop() || 'document');
            }
            renderMarkdown(text, name);
        } catch (err) {
            console.error('openUri failed:', err);
            showError('Could not open file', err && err.message ? err.message : String(err));
        }
    }

    function fileNameFromUri(uri) {
        try {
            const p = new URL(uri, location.href).pathname;
            return decodeURIComponent(p.split('/').pop() || 'document');
        } catch (e) {
            return uri.split('/').pop() || 'document';
        }
    }

    // ---- Browse: directory navigator (ticket 04 decision) ----------------------
    let browsePath = BROWSE_ROOT;
    let currentDirs = [];   // parallel with data-dir indices
    let currentFiles = [];  // parallel with data-file indices

    async function browseFiles() {
        const Files = Capacitor.Plugins.Filesystem;   // injected by the native bridge (no bundler in this app)
        try {
            browsePath = BROWSE_ROOT;
            await renderDir(Files, null);
        } catch (error) {
            console.error('Browse error:', error);
            showError('Could not browse', error.message || String(error));
        }
    }

    async function renderDir(Files, childName) {
        const path = childName != null ? browsePath + '/' + childName : browsePath;
        let entries;
        try {
            const result = await Files.readdir({ path: path });
            entries = result.files || [];
        } catch (e) {
            console.error('readdir failed:', e);
            showError('Could not list ' + displayName(path),
                (e.message ? e.message : String(e)) + ' — on Android 13+ grant "All files access" to MD Reader');
            return;
        }

        const byName = (a, b) => a.name.localeCompare(b.name, undefined, { numeric: true });
        currentDirs = entries.filter(f => f.type === 'directory').sort(byName);
        currentFiles = entries.filter(f => f.type === 'file' && MD_EXT_RE.test(f.name)).sort(byName);
        browsePath = path;

        let html = '<div class="nav-bar">' + escapeHtml(displayName(path)) + '</div>';

        // Up row: go one level up, or home from the root.
        const upLabel = canGoUp(path) ? '⬆️ ' + displayName(parentPath(path)) : '🏠 Home';
        html += '<div class="file-item" data-up="1">' +
            '<span class="file-icon">⬅️</span>' +
            '<div class="file-info"><div class="file-name">' + escapeHtml(upLabel) + '</div></div></div>';

        currentDirs.forEach((d, i) => {
            html += '<div class="file-item" data-dir="' + i + '">' +
                '<span class="file-icon">📁</span>' +
                '<div class="file-info"><div class="file-name">' + escapeHtml(d.name) + '</div></div></div>';
        });

        currentFiles.forEach((f, i) => {
            const size = f.size != null ? formatSize(f.size) : '';
            html += '<div class="file-item" data-file="' + i + '">' +
                '<span class="file-icon">📝</span>' +
                '<div class="file-info"><div class="file-name">' + escapeHtml(f.name) + '</div>' +
                (size ? '<div class="file-size">' + size + '</div>' : '') + '</div></div>';
        });

        if (currentDirs.length === 0 && currentFiles.length === 0) {
            html += '<div class="empty-state small"><p>No folders or .md files here</p></div>';
        } else {
            html += '<div class="nav-hint">Tap a file to read it · tap a folder to open it</div>';
        }

        contentArea.innerHTML = html;
        contentArea.scrollTop = 0;

        const upEl = contentArea.querySelector('[data-up="1"]');
        if (upEl) {
            upEl.addEventListener('click', async () => {
                browsePath = canGoUp(path) ? parentPath(path) : BROWSE_ROOT;
                await renderDir(Files, null);
            });
        }
        contentArea.querySelectorAll('[data-dir]').forEach(el => {
            el.addEventListener('click', async () => {
                const i = parseInt(el.getAttribute('data-dir'), 10);
                if (currentDirs[i]) await renderDir(Files, currentDirs[i].name);
            });
        });
        contentArea.querySelectorAll('[data-file]').forEach(el => {
            el.addEventListener('click', async () => {
                const i = parseInt(el.getAttribute('data-file'), 10);
                const f = currentFiles[i];
                if (!f) return;
                try {
                    const r = await Files.readFile({ path: browsePath + '/' + f.name, encoding: 'utf8' });
                    renderMarkdown(r.data, f.name);
                } catch (error) {
                    showError('Could not open', error.message || String(error));
                }
            });
        });
    }

    function canGoUp(path) {
        const parent = path.replace(/\/[^/]+$/, '');
        return parent !== '' && parent !== '/' && parent !== path;
    }
    function parentPath(path) {
        return path.replace(/\/[^/]+$/, '') || '/';
    }
    function displayName(p) {
        if (p === BROWSE_ROOT) return '📥 Downloads';
        const seg = p.replace(/\/+$/, '').split('/').pop();
        return seg || '/';
    }

    // ---- Open File fallback (input[type=file]) + drag-and-drop ------------------
    fileBtn.addEventListener('click', () => fileInput.click());
    browseBtn.addEventListener('click', browseFiles);

    fileInput.addEventListener('change', (e) => {
        const file = e.target.files && e.target.files[0];
        if (file) loadLocalFile(file);
    });

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
        const file = e.dataTransfer.files && e.dataTransfer.files[0];
        if (file) loadLocalFile(file);
    });

    function loadLocalFile(file) {
        const reader = new FileReader();
        reader.onload = (event) => renderMarkdown(event.target.result, file.name);
        reader.readAsText(file);
    }

    // ---- rendering ---------------------------------------------------------------
    function renderMarkdown(markdown, filename) {
        marked.setOptions({ breaks: true, gfm: true, headerIds: false });
        const html = marked.parse(markdown);

        contentArea.innerHTML =
            '<div class="md-content">' +
            '<h2>' + escapeHtml(filename) + '</h2>' +
            html +
            '</div>';
        contentArea.scrollTop = 0;
    }

    function showError(title, detail) {
        contentArea.innerHTML =
            '<div class="error-state"><p><strong>' + escapeHtml(title) + '</strong></p>' +
            (detail ? '<p class="err-detail">' + escapeHtml(detail) + '</p>' : '') +
            '</div>';
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function formatSize(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / 1024 / 1024).toFixed(1) + ' MB';
    }

    // Run the stored-URI safety net once on load.
    checkStored();
});
