document.getElementById('ai-recommend').addEventListener('submit', function () {
    document.getElementById('generate').disabled = true;
    document.getElementById('ai-loading').hidden = false;
});
// Restore controls when returning through browser history.
window.addEventListener('pageshow', function (event) {
    if (event.persisted) window.location.reload();
});
