(element) => {
    try {
        const computedStyle = window.getComputedStyle(element);
        return {
            color: computedStyle.color || 'rgb(0, 0, 0)',
            backgroundColor: computedStyle.backgroundColor || 'rgb(255, 255, 255)'
        };
    } catch (e) {
        return {
            color: 'rgb(0, 0, 0)',
            backgroundColor: 'rgb(255, 255, 255)'
        };
    }
}
