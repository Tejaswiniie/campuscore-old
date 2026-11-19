/*
 * Student form: Department -> Academic year -> Section.
 * The section list is fetched from the server, so it always shows exactly the sections the
 * admin has registered for the chosen department and year. The server checks the choice again.
 */
(function () {
  'use strict';

  var department = document.getElementById('departmentId');
  var year = document.getElementById('yearOfStudy');
  var section = document.getElementById('sectionId');
  var hint = document.getElementById('sectionHint');
  if (!department || !year || !section) { return; }

  var url = section.getAttribute('data-url');
  var latest = 0; // lets a newer selection cancel an older, slower request

  function say(text) {
    if (hint) { hint.textContent = text || ''; }
  }

  function reset(placeholder) {
    section.innerHTML = '';
    var option = document.createElement('option');
    option.value = '';
    option.textContent = placeholder;
    section.appendChild(option);
    section.value = '';
    section.disabled = true;
  }

  function refresh(wanted) {
    var token = ++latest;
    if (!department.value || !year.value) {
      reset('Select department and year first');
      say('');
      return;
    }
    reset('Loading sections...');
    say('');
    fetch(url + '?departmentId=' + encodeURIComponent(department.value) + '&year=' + encodeURIComponent(year.value),
          { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
      .then(function (response) {
        if (!response.ok) { throw new Error('request failed'); }
        return response.json();
      })
      .then(function (items) {
        if (token !== latest) { return; }
        if (!items.length) {
          reset('No sections available');
          say('No sections available');
          return;
        }
        section.innerHTML = '';
        var first = document.createElement('option');
        first.value = '';
        first.textContent = 'Select a section';
        section.appendChild(first);
        items.forEach(function (item) {
          var option = document.createElement('option');
          option.value = item.id;
          option.textContent = 'Section ' + item.name;
          section.appendChild(option);
        });
        section.disabled = false;
        var stillValid = wanted && items.some(function (item) { return String(item.id) === String(wanted); });
        section.value = stillValid ? String(wanted) : '';
        say('');
      })
      .catch(function () {
        if (token === latest) {
          reset('Could not load sections');
          say('Could not load sections. Reload the page and try again.');
        }
      });
  }

  department.addEventListener('change', function () { refresh(null); });
  year.addEventListener('change', function () { refresh(null); });
  refresh(section.value || null);
})();
