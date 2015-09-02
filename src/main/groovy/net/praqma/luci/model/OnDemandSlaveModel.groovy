package net.praqma.luci.model

class OnDemandSlaveModel extends BaseServiceModel {

    String dockerImage

    String slaveName

    void dockerImage(String image) {
        this.dockerImage = image
    }
}
