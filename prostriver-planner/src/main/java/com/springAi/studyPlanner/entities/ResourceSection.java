package com.springAi.studyPlanner.entities;

import java.util.List;

public class ResourceSection {

    private Resource documentation;
    private List<Resource> youtube;
    private Resource extra;

    public Resource getDocumentation() {
        return documentation;
    }

    public void setDocumentation(Resource documentation) {
        this.documentation = documentation;
    }

    public List<Resource> getYoutube() {
        return youtube;
    }

    public void setYoutube(List<Resource> youtube) {
        this.youtube = youtube;
    }

    public Resource getExtra() {
        return extra;
    }

    public void setExtra(Resource extra) {
        this.extra = extra;
    }
}