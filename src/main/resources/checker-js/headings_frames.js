() => {
    const frames = [];

    // frameset, frame 태그 검사
    document.querySelectorAll('frame, frameset').forEach(frame => {
        const title = frame.getAttribute('title') || frame.getAttribute('name') || '';
        frames.push({
            tag: frame.tagName.toLowerCase(),
            hasTitle: !!title.trim(),
            title: title.trim(),
            src: frame.src || 'no src',
            html: frame.outerHTML
        });
    });

    return frames;
}
