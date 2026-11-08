() => {
    const issues = [];
    const title = document.title || '';

    // 페이지 제목의 특수 기호 반복 검사
    const specialSymbolPattern = /([!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`])\1{2,}/g;
    if (specialSymbolPattern.test(title)) {
        issues.push({
            type: 'page_title',
            text: title,
            issue: 'repeated_special_symbols'
        });
    }

    // 제목 태그의 특수 기호 반복 검사
    document.querySelectorAll('h1, h2, h3, h4, h5, h6').forEach(heading => {
        const text = heading.textContent.trim();
        if (specialSymbolPattern.test(text)) {
            issues.push({
                type: 'heading',
                text: text,
                tag: heading.tagName.toLowerCase(),
                issue: 'repeated_special_symbols'
            });
        }
    });

    // iframe, frame 제목의 특수 기호 반복 검사
    document.querySelectorAll('iframe, frame').forEach(frame => {
        const titleAttr = frame.getAttribute('title') || '';
        if (titleAttr && specialSymbolPattern.test(titleAttr)) {
            issues.push({
                type: 'frame_title',
                text: titleAttr,
                issue: 'repeated_special_symbols'
            });
        }
    });

    return issues;
}
