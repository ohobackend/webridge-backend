() => {
    const title = document.title;
    return {
        exists: !!title,
        text: title ? title.trim() : '',
        empty: !title || !title.trim(),
        length: title ? title.trim().length : 0
    };
}
