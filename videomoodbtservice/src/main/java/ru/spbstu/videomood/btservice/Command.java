package ru.spbstu.videomood.btservice;

public enum Command {
    /**
     * Get list of available videos
     */
    LIST,

    /**
     * Select video with specified id and play it
     */
    PLAY,

    /**
     * Pause or unpause current video depend on its current state
     */
    PAUSE,

    /**
     * Switch to a next video
     */
    NEXT,

    /**
     * Switch to previous video
     */
    PREV,


    /**
     * Get data request
     */
    GET,

    /**
     * Change current video position
     */
    REWIND
}
