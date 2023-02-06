(() => {

    /**
     * This registers a command to clear the content in the new Task input.
     * (e.g. to clear it after successfully creating it).
     */
    window.addEventListener("clear-add-task", () => {
        document.querySelectorAll(".new-task").forEach((elm) => (elm.value = ""));
    });

    htmx.onLoad(function(content) {
        // make the Tasks in task-lists sortable
        let sortables = content.querySelectorAll(".sortable .task-list");
        for (let i = 0; i < sortables.length; i++) {
            let sortable = sortables[i];
            console.log(sortable)
            new Sortable(sortable, {
                animation: 150,
                ghostClass: 'blue-background-class'
            });
        }
    });

})();