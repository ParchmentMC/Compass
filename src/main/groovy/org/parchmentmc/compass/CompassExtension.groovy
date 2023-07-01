package org.parchmentmc.compass

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.DSLProperty
import net.minecraftforge.gdi.annotations.ProjectGetter
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.parchmentmc.compass.storage.io.MappingIOFormat

import javax.inject.Inject

@CompileStatic
abstract class CompassExtension {
    private final Project project

    @Inject
    CompassExtension(final Project project) {
        this.project = project
        getLauncherManifestURL().convention('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json');
        getProductionData().convention(project.layout.projectDirectory.dir('data'))
        getProductionDataFormat().convention(MappingIOFormat.ENIGMA_EXPLODED)
        getStagingData().convention(project.layout.projectDirectory.dir('staging'))
        getStagingDataFormat().convention(MappingIOFormat.ENIGMA_EXPLODED)
        getInputs().convention(project.layout.projectDirectory.dir('input'))
    }

    @ProjectGetter
    private Project getProject() {
        return this.@project
    }

    @DSLProperty
    abstract Property<String> getLauncherManifestURL()

    @DSLProperty
    abstract DirectoryProperty getProductionData()

    @DSLProperty
    abstract Property<MappingIOFormat> getProductionDataFormat()

    @DSLProperty
    abstract Property<String> getVersion()

    @DSLProperty
    abstract DirectoryProperty getStagingData()

    @DSLProperty
    abstract Property<MappingIOFormat> getStagingDataFormat()

    @DSLProperty
    abstract DirectoryProperty getInputs()
}
