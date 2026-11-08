() => {
    const errors = [];

    // img 태그 src 속성 확인
    document.querySelectorAll('img:not([src])').forEach(img => {
        errors.push({
            type: 'img 태그에 src 속성 누락',
            html: img.outerHTML
        });
    });

    // label과 input 연결 확인
    document.querySelectorAll('input[type="text"], input[type="email"], input[type="password"]').forEach(input => {
        const id = input.id;
        if (!id || !document.querySelector(`label[for="${id}"]`)) {
            if (!input.getAttribute('aria-label') && !input.getAttribute('aria-labelledby')) {
                errors.push({
                    type: 'form 요소에 적절한 레이블 누락',
                    html: input.outerHTML,
                    inputId: id || 'no-id'
                });
            }
        }
    });

    return errors;
}
