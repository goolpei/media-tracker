// Global Application State
let allMedia = [];
let currentFilter = 'All';
let isEditing = false;

// DOM Element References
const mediaTableBody = document.getElementById('media-table-body');
const emptyState = document.getElementById('empty-state');
const searchInput = document.getElementById('search-input');
const navButtons = document.querySelectorAll('.nav-btn');
const openAddModalBtn = document.getElementById('open-add-modal-btn');
const closeModalBtn = document.getElementById('close-modal-btn');
const cancelModalBtn = document.getElementById('cancel-modal-btn');
const mediaModal = document.getElementById('media-modal');
const mediaForm = document.getElementById('media-form');
const modalTitle = document.getElementById('modal-title');
const mediaIdInput = document.getElementById('media-id');
const mediaTypeSelect = document.getElementById('media-type');
const mediaTitleInput = document.getElementById('media-title');
const mediaGenreInput = document.getElementById('media-genre');
const mediaRatingInput = document.getElementById('media-rating');
const mediaStatusSelect = document.getElementById('media-status');
const mediaExtraInput = document.getElementById('media-extra');
const extraLabel = document.getElementById('extra-label');
const mediaLinkInput = document.getElementById('media-link');

// Metric Unit Mappings
const metricLabels = {
    'Anime': 'Episodes',
    'Movie': 'Duration (mins)',
    'Book': 'Pages',
    'Manga': 'Chapters',
    'Manhwa': 'Chapters'
};

const metricUnits = {
    'Anime': 'eps',
    'Movie': 'mins',
    'Book': 'pages',
    'Manga': 'chaps',
    'Manhwa': 'chaps'
};

// Application Entry Point
document.addEventListener('DOMContentLoaded', () => {
    fetchMedia();
    setupEventListeners();
});

// Event Listeners Configuration
function setupEventListeners() {
    // Sidebar Category Filter
    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            navButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            currentFilter = btn.getAttribute('data-filter');
            renderTable();
        });
    });

    // Real-time Search Input
    searchInput.addEventListener('input', () => {
        renderTable();
    });

    // Modal Control Triggers
    openAddModalBtn.addEventListener('click', () => openModal(false));
    closeModalBtn.addEventListener('click', closeModal);
    cancelModalBtn.addEventListener('click', closeModal);

    mediaModal.addEventListener('click', (e) => {
        if (e.target === mediaModal) closeModal();
    });

    // Dynamic Extra Label Switcher
    mediaTypeSelect.addEventListener('change', (e) => {
        updateExtraLabel(e.target.value);
    });

    // Form Handler
    mediaForm.addEventListener('submit', handleFormSubmit);
}

// Fetch All Media Records from Javalin REST API
async function fetchMedia() {
    try {
        const response = await fetch('/api/media');
        if (!response.ok) throw new Error('Failed to fetch media records.');
        allMedia = await response.json();
        renderTable();
    } catch (error) {
        console.error('Error fetching media:', error);
        showEmptyState(true);
    }
}

// Render Data Table Rows Based on Active Category & Search Term
function renderTable() {
    const query = searchInput.value.toLowerCase().trim();

    const filteredMedia = allMedia.filter(item => {
        const matchesCategory = (currentFilter === 'All') || (item.mediaType === currentFilter);
        const matchesSearch = 
            item.title.toLowerCase().includes(query) ||
            item.id.toLowerCase().includes(query) ||
            (item.genre && item.genre.toLowerCase().includes(query));

        return matchesCategory && matchesSearch;
    });

    mediaTableBody.innerHTML = '';

    if (filteredMedia.length === 0) {
        showEmptyState(true);
        return;
    }

    showEmptyState(false);

    filteredMedia.forEach(item => {
        const tr = document.createElement('tr');
        const statusClass = getStatusBadgeClass(item.status);
        const unit = metricUnits[item.mediaType] || '';
        const formattedExtra = item.extraInfo !== undefined && item.extraInfo !== null ? `${item.extraInfo} ${unit}` : '-';

        tr.innerHTML = `
            <td><strong>${escapeHtml(item.id)}</strong></td>
            <td>${escapeHtml(item.title)}</td>
            <td><span class="badge badge-type">${escapeHtml(item.mediaType)}</span></td>
            <td>${escapeHtml(item.genre || '-')}</td>
            <td><i class="fa-solid fa-star" style="color: #f59e0b; margin-right: 4px;"></i>${item.rating ? item.rating.toFixed(1) : 'N/A'}</td>
            <td><span class="badge ${statusClass}">${escapeHtml(item.status)}</span></td>
            <td>${escapeHtml(formattedExtra)}</td>
            <td class="text-right">
                <div class="action-btn-group">
                    ${item.sourceLink ? `
                        <button class="icon-btn redirect" title="Open Source Link" onclick="triggerRedirect('${item.id}')">
                            <i class="fa-solid fa-arrow-up-right-from-square"></i>
                        </button>
                    ` : ''}
                    <button class="icon-btn edit" title="Edit Item" onclick="openEditModal('${item.id}')">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button class="icon-btn delete" title="Delete Item" onclick="deleteMedia('${item.id}')">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </div>
            </td>
        `;

        mediaTableBody.appendChild(tr);
    });
}

// Modal Operation Handlers
function openModal(editMode = false, item = null) {
    isEditing = editMode;
    mediaForm.reset();

    if (editMode && item) {
        modalTitle.innerText = 'Edit Media';
        mediaIdInput.value = item.id;
        mediaIdInput.disabled = true;
        mediaTypeSelect.value = item.mediaType;
        mediaTitleInput.value = item.title;
        mediaGenreInput.value = item.genre || '';
        mediaRatingInput.value = item.rating || '';
        mediaStatusSelect.value = item.status || 'Unconsumed';
        mediaExtraInput.value = item.extraInfo || '';
        mediaLinkInput.value = item.sourceLink || '';
    } else {
        modalTitle.innerText = 'Add New Media';
        mediaIdInput.disabled = false;
    }

    updateExtraLabel(mediaTypeSelect.value);
    mediaModal.classList.remove('hidden');
}

function closeModal() {
    mediaModal.classList.add('hidden');
    mediaForm.reset();
    isEditing = false;
}

function updateExtraLabel(type) {
    extraLabel.innerText = metricLabels[type] || 'Extra Metric';
}

function openEditModal(id) {
    const item = allMedia.find(m => m.id === id);
    if (item) {
        openModal(true, item);
    }
}

// Create & Update Submission Logic
async function handleFormSubmit(e) {
    e.preventDefault();

    const payload = {
        id: mediaIdInput.value.trim(),
        mediaType: mediaTypeSelect.value,
        title: mediaTitleInput.value.trim(),
        genre: mediaGenreInput.value.trim(),
        rating: parseFloat(mediaRatingInput.value) || 0.0,
        status: mediaStatusSelect.value,
        extraInfo: parseInt(mediaExtraInput.value, 10) || 0,
        sourceLink: mediaLinkInput.value.trim()
    };

    const endpoint = isEditing ? '/api/media/update' : '/api/media/add';
    const method = isEditing ? 'PUT' : 'POST';

    try {
        const response = await fetch(endpoint, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Failed to save media item.');
        }

        closeModal();
        fetchMedia();
    } catch (error) {
        alert(`Error saving media: ${error.message}`);
    }
}

// Delete Request Handler
async function deleteMedia(id) {
    if (!confirm(`Are you sure you want to delete media item ID: ${id}?`)) return;

    try {
        const response = await fetch(`/api/media/${encodeURIComponent(id)}`, {
            method: 'DELETE'
        });

        if (!response.ok) throw new Error('Failed to delete media record.');

        fetchMedia();
    } catch (error) {
        alert(`Delete failed: ${error.message}`);
    }
}

// Desktop Redirection Trigger
async function triggerRedirect(id) {
    try {
        const response = await fetch(`/api/media/redirect/${encodeURIComponent(id)}`, {
            method: 'POST'
        });

        if (!response.ok) throw new Error('Could not trigger external link.');
    } catch (error) {
        console.error('Redirect error:', error);
    }
}

// Utility Helpers
function getStatusBadgeClass(status) {
    switch (status) {
        case 'Unconsumed': return 'badge-unconsumed';
        case 'Currently Consuming': return 'badge-consuming';
        case 'Finished': return 'badge-finished';
        default: return 'badge-unconsumed';
    }
}

function showEmptyState(show) {
    if (show) {
        emptyState.classList.remove('hidden');
    } else {
        emptyState.classList.add('hidden');
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}