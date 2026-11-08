async () => {
    await new Promise((resolve) => {
        let totalHeight = 0;
        let distance = 300;
        let timer = setInterval(() => {
            let scrollHeight = document.body ? document.body.scrollHeight : 0;
            window.scrollBy(0, distance);
            totalHeight += distance;

            if (totalHeight >= scrollHeight || totalHeight > 15000) {
                clearInterval(timer);
                resolve();
            }
        }, 100);
    });
}
