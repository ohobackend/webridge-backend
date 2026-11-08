() => {
    const popups = [];

    // iframe 검사
    document.querySelectorAll('iframe').forEach(iframe => {
        const title = iframe.getAttribute('title') || iframe.getAttribute('name') || '';
        popups.push({
            type: 'iframe',
            hasTitle: !!title.trim(),
            title: title.trim(),
            src: iframe.src || 'no src',
            html: iframe.outerHTML
        });
    });

    // object, embed 태그도 검사
    document.querySelectorAll('object, embed').forEach(obj => {
        const title = obj.getAttribute('title') || obj.getAttribute('name') || '';
        popups.push({
            type: obj.tagName.toLowerCase(),
            hasTitle: !!title.trim(),
            title: title.trim(),
            html: obj.outerHTML
        });
    });

    return popups;
}
