() => {
    const ids = {};
    const duplicates = [];
    document.querySelectorAll('[id]').forEach(el => {
        if (ids[el.id]) {
            duplicates.push({
                id: el.id,
                html: el.outerHTML
            });
        } else {
            ids[el.id] = true;
        }
    });
    return duplicates;
}
