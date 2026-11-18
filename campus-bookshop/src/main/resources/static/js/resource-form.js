(() => {
    const type = document.getElementById('resourceType');
    const fields = document.getElementById('saleFields');
    function update() {
        const sale = type.value === '图书';
        fields.hidden = !sale;
        document.getElementById('browseOnly').hidden = sale;
        fields.querySelectorAll('input').forEach(input => { input.disabled = !sale; input.required = sale; });
    }
    type.addEventListener('change', update);
    update();
})();
