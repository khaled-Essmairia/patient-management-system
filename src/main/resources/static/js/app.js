document.addEventListener('DOMContentLoaded', function () {
  var sb = document.getElementById('sidebar'), btn = document.getElementById('menuBtn');
  if (btn && sb) btn.addEventListener('click', function () { sb.classList.toggle('open'); });

  // "View" buttons expand the detail row that follows the clicked row
  document.querySelectorAll('[data-toggle-detail]').forEach(function (b) {
    b.addEventListener('click', function () {
      var next = b.closest('tr').nextElementSibling;
      if (next && next.classList.contains('detail')) next.hidden = !next.hidden;
    });
  });

  // confirmation before destructive POSTs
  document.addEventListener('submit', function (e) {
    var msg = e.target.dataset && e.target.dataset.confirm;
    if (msg && !confirm(msg)) e.preventDefault();
  });

  setTimeout(function () {
    document.querySelectorAll('.alert.auto').forEach(function (a) { a.style.display = 'none'; });
  }, 6000);
});
