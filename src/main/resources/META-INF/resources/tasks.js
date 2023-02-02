(() => {

    /**
     * This registers a command to clear the content in the new Task input.
     * (e.g. to clear it after successfully creating it).
     */
    window.addEventListener("clear-add-task", () => {
        document.querySelectorAll(".new-task").forEach((elm) => (elm.value = ""));
    });

})();