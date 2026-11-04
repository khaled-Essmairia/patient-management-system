/* Patient file: editable line grids (consultations, appointments, invoices) with page size, search and pagination. */
// Runs on DOMContentLoaded: this script sits inside <main>, i.e. BEFORE layout.html loads paging.js at the end of <body>.
// Without the wait, window.Paging was still undefined, the script crashed and the + buttons / pagination never worked.
document.addEventListener('DOMContentLoaded', function () {
  var form = document.getElementById('patientForm');
  if (!form) return;
  var grids = [];

  function rowText(tr) {
    var parts = [];
    tr.querySelectorAll('input,select,textarea').forEach(function (el) {
      if (el.type === 'hidden') return;
      if (el.tagName === 'SELECT') {
        var op = el.options[el.selectedIndex];
        if (op) parts.push(op.textContent);
      } else parts.push(el.value);
    });
    return parts.join(' ');
  }

  document.querySelectorAll('table[data-grid]').forEach(function (table) {
    var tbody = table.querySelector('tbody');
    var tpl = table.querySelector('template');
    var emptyRow = tbody.querySelector('tr.empty');
    var wrapper = table.closest('.table-scroll');
    var paging;

    function rows() { return Array.prototype.slice.call(tbody.querySelectorAll('tr.line')); }

    function add() {
      paging.clearSearch();
      var node = tpl.content.firstElementChild.cloneNode(true);
      var first = tbody.querySelector('tr.line');
      if (first) tbody.insertBefore(node, first);
      else if (emptyRow) emptyRow.parentNode.insertBefore(node, emptyRow.nextSibling);
      else tbody.appendChild(node);
      paging.first();
      var input = node.querySelector('input:not([type=hidden]):not(.ro)');
      if (input) input.focus();
    }

    paging = window.Paging({
      before: wrapper,
      after: wrapper,
      size: 5,
      units: rows,
      text: rowText,
      show: function (tr, visible) { tr.hidden = !visible; },
      onAdd: add,
      onRender: function (all) {
        all.forEach(function (tr, i) {
          var ln = tr.querySelector('.ln');
          if (ln) ln.textContent = i + 1;
        });
        if (emptyRow) emptyRow.hidden = all.length > 0;
        var count = document.querySelector('[data-count="' + table.dataset.grid + '"]');
        if (count) count.textContent = all.length;
        form.dispatchEvent(new CustomEvent('grid-changed'));
      }
    });

    table.addEventListener('click', function (e) {
      if (e.target.closest('[data-add]')) { add(); return; }
      var rm = e.target.closest('[data-remove]');
      if (rm) {
        var tr = rm.closest('tr.line');
        var idField = tr.querySelector('input[name$=".id"]');
        if (idField && idField.value && !confirm((window.I18N && window.I18N.removeLine) || 'Remove this line? It is deleted from the file when you save.')) return;
        tr.remove();
        paging.render();
      }
    });

    grids.push({ table: table, rows: rows, paging: paging });
  });

  // a required field sitting on another page (or filtered out): jump to it so the browser can show the message
  form.addEventListener('invalid', function (e) {
    var tr = e.target.closest && e.target.closest('tr.line');
    if (!tr || !tr.hidden) return;
    grids.forEach(function (g) { if (g.table.contains(tr)) g.paging.reveal(tr); });
  }, true);

  // renumber the indexed field names (consultations[0].x ...) so the server never sees gaps
  form.addEventListener('submit', function () {
    grids.forEach(function (g) {
      g.rows().forEach(function (tr, i) {
        tr.querySelectorAll('[name]').forEach(function (el) {
          el.name = el.name.replace(/\[\d+\]/, '[' + i + ']');
        });
      });
    });
  });

  // invoice maths (the VAT rate of existing invoices is kept in a hidden field)
  var inv = document.querySelector('table[data-grid="invoices"]');
  function num(v) { var n = parseFloat(v); return isNaN(n) ? 0 : n; }
  function fmt(n) { return n.toFixed(2); }
  function totals() {
    var all = 0, paid = 0, due = 0;
    inv.querySelectorAll('tr.line').forEach(function (tr) {
      var h = num(tr.querySelector('.u-price').value) * Math.max(0, parseInt(tr.querySelector('.qty').value, 10) || 0);
      var vatEl = tr.querySelector('.vat');
      var t = h + (vatEl ? h * num(vatEl.value) / 100 : 0);
      tr.querySelector('.l-ttc').value = fmt(t);
      all += t;
      if (tr.querySelector('.l-status').value === 'PAID') paid += t; else due += t;
    });
    var set = function (k, val) { var el = document.querySelector('[data-total="' + k + '"]'); if (el) el.value = fmt(val); };
    set('ttc', all); set('paid', paid); set('due', due);
  }
  if (inv) {
    inv.addEventListener('input', totals);
    inv.addEventListener('change', totals);
    form.addEventListener('grid-changed', totals);
    totals();
  }
});
