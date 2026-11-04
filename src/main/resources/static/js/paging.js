/*
 * Paging: client-side pagination + search for any group of "units" (table rows, grid lines).
 *   - page size selector (5 / 10 / 15 / 20 / 30 / 50)
 *   - one search box that looks in every column of the row
 *   - page buttons and a "Showing x-y of n" line
 * Used by the list pages (tables with data-paged) and by the grids of the patient file.
 */
(function () {
  var SIZES = [5, 10, 15, 20, 30, 50];
  // texts come from the page (window.I18N) so they follow the language of the user
  function tx(key, fallback) { return (window.I18N && window.I18N[key]) || fallback; }

  function h(tag, cls, text) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text) e.textContent = text;
    return e;
  }

  window.Paging = function (o) {
    var size = o.size || 10, page = 1, query = '';

    // ---- tool bar (above the table)
    var bar = h('div', 'table-tools');
    var sizeLabel = h('label', 'tt-size', tx('show', 'Show') + ' ');
    var select = h('select');
    SIZES.forEach(function (n) {
      var op = h('option', '', String(n));
      op.value = n;
      if (n === size) op.selected = true;
      select.appendChild(op);
    });
    sizeLabel.appendChild(select);
    sizeLabel.appendChild(document.createTextNode(' ' + tx('perPage', 'per page')));
    bar.appendChild(sizeLabel);

    if (o.onAdd) {
      var add = h('button', 'btn small tt-add', '+ ' + tx('add', 'Add'));
      add.type = 'button';
      add.title = tx('addLine', 'Add a new line');
      add.addEventListener('click', o.onAdd);
      bar.appendChild(add);
    }

    var search = h('input', 'tt-search');
    search.type = 'search';
    search.placeholder = tx('search', 'Search in this table...') + '...';
    search.setAttribute('aria-label', tx('search', 'Search in this table'));
    bar.appendChild(search);

    // ---- footer (below the table)
    var foot = h('div', 'table-foot');
    var info = h('span', 'tt-info');
    var pages = h('div', 'tt-pages');
    foot.appendChild(info);
    foot.appendChild(pages);

    o.before.parentNode.insertBefore(bar, o.before);
    o.after.parentNode.insertBefore(foot, o.after.nextSibling);

    function terms() { return query.toLowerCase().split(/\s+/).filter(Boolean); }

    function matches(unit, t) {
      if (!t.length) return true;
      var text = o.text(unit).toLowerCase();
      return t.every(function (w) { return text.indexOf(w) !== -1; });
    }

    function button(label, enabled, cls, fn) {
      var b = h('button', cls || '', label);
      b.type = 'button';
      b.disabled = !enabled;
      if (fn) b.addEventListener('click', fn);
      return b;
    }

    function drawPages(total) {
      pages.innerHTML = '';
      if (total <= 1) return;
      pages.appendChild(button('\u2039', page > 1, '', function () { page--; render(); }));
      var last = 0;
      for (var p = 1; p <= total; p++) {
        if (p === 1 || p === total || Math.abs(p - page) <= 1) {
          if (last && p - last > 1) pages.appendChild(h('span', 'tt-gap', '\u2026'));
          (function (p) {
            pages.appendChild(button(String(p), true, p === page ? 'on' : '', function () { page = p; render(); }));
          })(p);
          last = p;
        }
      }
      pages.appendChild(button('\u203A', page < total, '', function () { page++; render(); }));
    }

    function render() {
      var all = o.units(), t = terms(), matched = [];
      all.forEach(function (u) {
        if (matches(u, t)) matched.push(u); else o.show(u, false);
      });
      var total = Math.max(1, Math.ceil(matched.length / size));
      if (page > total) page = total;
      var start = (page - 1) * size, end = start + size;
      matched.forEach(function (u, i) { o.show(u, i >= start && i < end); });

      if (!all.length) info.textContent = tx('noEntries', 'No entries');
      else if (!matched.length) info.textContent = tx('noMatch', 'No match found');
      else info.textContent = tx('showing', 'Showing') + ' ' + (start + 1) + '\u2013' + Math.min(end, matched.length) + ' ' + tx('of', 'of') + ' ' + matched.length
          + (matched.length !== all.length ? ' (' + tx('filteredFrom', 'filtered from') + ' ' + all.length + ')' : '');
      drawPages(total);
      if (o.onRender) o.onRender(all, matched);
    }

    select.addEventListener('change', function () { size = parseInt(select.value, 10); page = 1; render(); });
    search.addEventListener('input', function () { query = search.value; page = 1; render(); });

    var api = {
      render: render,
      first: function () { page = 1; render(); },
      clearSearch: function () { query = ''; search.value = ''; },
      /** Make sure a given unit is visible (clears the search and jumps to its page). */
      reveal: function (unit) {
        if (!matches(unit, terms())) api.clearSearch();
        var idx = o.units().filter(function (u) { return matches(u, terms()); }).indexOf(unit);
        if (idx >= 0) { page = Math.floor(idx / size) + 1; render(); }
      }
    };
    render();
    return api;
  };

  // ---- list pages: <div class="card table-wrap" data-paged> with <table class="list"> and one <tbody data-row> per record
  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-paged]').forEach(function (card) {
      var table = card.querySelector('table.list');
      if (!table) return;
      window.Paging({
        before: table,
        after: table,
        size: 10,
        units: function () { return Array.prototype.slice.call(table.querySelectorAll('tbody[data-row]')); },
        text: function (tbody) {
          return Array.prototype.map.call(tbody.querySelectorAll('td'), function (td) {
            return td.classList.contains('actions') ? '' : td.textContent;
          }).join(' ');
        },
        show: function (tbody, visible) { tbody.hidden = !visible; }
      });
    });
  });
})();
