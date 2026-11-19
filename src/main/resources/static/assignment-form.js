/*
 * Assignment form: shows the chosen file and checks its type and size before upload.
 * This is only a convenience. The server checks the file again and is the one that decides.
 */
(function () {
  'use strict';

  var input = document.getElementById('file');
  var chosen = document.getElementById('file-chosen');
  if (!input || !chosen) { return; }

  var allowed = ['pdf', 'docx', 'pptx', 'xlsx', 'txt'];
  var maxBytes = parseInt(input.getAttribute('data-max-bytes') || '0', 10);

  function readable(bytes) {
    if (bytes < 1024) { return bytes + ' B'; }
    if (bytes < 1024 * 1024) { return Math.round(bytes / 1024) + ' KB'; }
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  function refuse(message) {
    chosen.textContent = message;
    chosen.className = 'chosen bad';
    input.value = '';
  }

  input.addEventListener('change', function () {
    chosen.className = 'chosen';
    var file = input.files && input.files[0];
    if (!file) { chosen.textContent = ''; return; }
    var extension = file.name.indexOf('.') >= 0 ? file.name.split('.').pop().toLowerCase() : '';
    if (allowed.indexOf(extension) === -1) {
      refuse('This file type is not allowed. Use PDF, DOCX, PPTX, XLSX or TXT.');
      return;
    }
    if (file.size === 0) { refuse('The file is empty.'); return; }
    if (maxBytes > 0 && file.size > maxBytes) {
      refuse('The file is too large (' + readable(file.size) + '). The limit is ' + readable(maxBytes) + '.');
      return;
    }
    chosen.textContent = 'Selected: ' + file.name + ' (' + readable(file.size) + ')';
  });
})();
