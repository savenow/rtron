/*
 * Copyright 2019-2023 Chair of Geoinformatics, Technical University of Munich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rtron.transformer.converter.opendrive2roadspaces.roadspaces

import arrow.core.NonEmptyList
import arrow.core.None
import arrow.core.Option
import arrow.core.flatten
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import arrow.core.some
import arrow.core.toNonEmptyListOrNull
import io.rtron.io.messages.ContextMessageList
import io.rtron.io.messages.DefaultMessageList
import io.rtron.io.messages.mergeMessageLists
import io.rtron.math.geometry.curved.threed.point.CurveRelativeVector3D
import io.rtron.math.geometry.euclidean.threed.AbstractGeometry3D
import io.rtron.math.geometry.euclidean.threed.Rotation3D
import io.rtron.math.geometry.euclidean.threed.curve.Curve3D
import io.rtron.model.opendrive.objects.EObjectType
import io.rtron.model.opendrive.signal.RoadSignals
import io.rtron.model.opendrive.signal.RoadSignalsSignal
import io.rtron.model.roadspaces.identifier.RoadspaceIdentifier
import io.rtron.model.roadspaces.identifier.RoadspaceObjectIdentifier
import io.rtron.model.roadspaces.roadspace.attribute.AttributeList
import io.rtron.model.roadspaces.roadspace.attribute.attributes
import io.rtron.model.roadspaces.roadspace.objects.RoadObjectType
import io.rtron.model.roadspaces.roadspace.objects.RoadspaceObject
import io.rtron.transformer.converter.opendrive2roadspaces.Opendrive2RoadspacesParameters
import io.rtron.transformer.converter.opendrive2roadspaces.geometry.Curve3DBuilder
import io.rtron.transformer.converter.opendrive2roadspaces.geometry.Solid3DBuilder
import io.rtron.transformer.converter.opendrive2roadspaces.geometry.Surface3DBuilder
import io.rtron.transformer.converter.opendrive2roadspaces.geometry.Vector3DBuilder
import io.rtron.model.opendrive.objects.RoadObjects as OpendriveRoadObjects
import io.rtron.model.opendrive.objects.RoadObjectsObject as OpendriveRoadObject
import io.rtron.model.opendrive.objects.RoadObjectsObjectRepeat as OpendriveRoadObjectRepeat

/**
 * Builder for [RoadspaceObject] which correspond to the OpenDRIVE road object class.
 */
class RoadspaceObjectBuilder(
    private val parameters: Opendrive2RoadspacesParameters
) {

    // Methods
    /**
     * Builds up a list of [RoadspaceObject].
     *
     * @param roadReferenceLine road reference line required to build the geometries of the road object
     * @param baseAttributes attributes attached to each road space object
     */
    fun buildRoadspaceObjects(
        roadspaceId: RoadspaceIdentifier,
        roadObjects: OpendriveRoadObjects,
        roadReferenceLine: Curve3D,
        baseAttributes: AttributeList
    ): ContextMessageList<List<RoadspaceObject>> {

        return roadObjects.roadObject
            .map { buildRoadObject(roadspaceId, it, roadReferenceLine, baseAttributes) }
            .mergeMessageLists()
            .map { it.flatten() }
    }

    private fun buildRoadObject(
        id: RoadspaceIdentifier,
        roadObject: OpendriveRoadObject,
        roadReferenceLine: Curve3D,
        baseAttributes: AttributeList
    ): ContextMessageList<NonEmptyList<RoadspaceObject>> {
        val messageList = DefaultMessageList()

        // get general object type and geometry representation
        val type = getObjectType(roadObject)

        // build attributes
        val attributes = baseAttributes +
            buildAttributes(roadObject) +
            buildAttributes(roadObject.curveRelativePosition) +
            buildAttributes(roadObject.referenceLinePointRelativeRotation)

        val roadObjectsFromRepeat = roadObject.repeat.map { currentRoadObjectRepeat ->
            val repeatIdentifier = currentRoadObjectRepeat.additionalId.toEither { IllegalStateException("Additional outline ID must be available.") }.getOrElse { throw it }

            val roadspaceObjectId = RoadspaceObjectIdentifier("${roadObject.id}_${repeatIdentifier.repeatIndex}", roadObject.name, id)
            val geometry = buildGeometries(roadObject, currentRoadObjectRepeat.some(), roadReferenceLine).handleMessageList { messageList += it }
            RoadspaceObject(roadspaceObjectId, type, geometry, attributes)
        }

        val roadObjects = if (roadObjectsFromRepeat.isEmpty()) {
            val roadspaceObjectId = RoadspaceObjectIdentifier(roadObject.id, roadObject.name, id)
            val geometry = buildGeometries(roadObject, None, roadReferenceLine).handleMessageList { messageList += it }
            nonEmptyListOf(RoadspaceObject(roadspaceObjectId, type, geometry, attributes))
        } else {
            roadObjectsFromRepeat.toNonEmptyListOrNull()!!
        }

        // build roadspace object
        return ContextMessageList(roadObjects, messageList)
    }

    private fun getObjectType(roadObject: OpendriveRoadObject): RoadObjectType = roadObject.type.fold({ RoadObjectType.NONE }, {
        when (it) {
            EObjectType.NONE -> RoadObjectType.NONE
            EObjectType.OBSTACLE -> RoadObjectType.OBSTACLE
            EObjectType.POLE -> RoadObjectType.POLE
            EObjectType.TREE -> RoadObjectType.TREE
            EObjectType.VEGETATION -> RoadObjectType.VEGETATION
            EObjectType.BARRIER -> RoadObjectType.BARRIER
            EObjectType.BUILDING -> RoadObjectType.BUILDING
            EObjectType.PARKING_SPACE -> RoadObjectType.PARKING_SPACE
            EObjectType.PATCH -> RoadObjectType.PATCH
            EObjectType.RAILING -> RoadObjectType.RAILING
            EObjectType.TRAFFIC_ISLAND -> RoadObjectType.TRAFFIC_ISLAND
            EObjectType.CROSSWALK -> RoadObjectType.CROSSWALK
            EObjectType.STREET_LAMP -> RoadObjectType.STREET_LAMP
            EObjectType.GANTRY -> RoadObjectType.GANTRY
            EObjectType.SOUND_BARRIER -> RoadObjectType.SOUND_BARRIER
            EObjectType.ROAD_MARK -> RoadObjectType.ROAD_MARK
        }
    })

    /**
     * Reads in the OpenDRIVE road object geometries and builds up the implemented geometries of [AbstractGeometry3D].
     *
     * @param roadObject road object source model
     * @param roadObjectRepeat considered repeat element for building complex geometries like parametric sweeps
     * @return list of transformed geometries
     */
    private fun buildGeometries(
        roadObject: OpendriveRoadObject,
        roadObjectRepeat: Option<OpendriveRoadObjectRepeat>,
        roadReferenceLine: Curve3D
    ): ContextMessageList<AbstractGeometry3D> {
        val messageList = DefaultMessageList()

        // affine transformation matrix at the curve point of the object
        val curveAffine = roadReferenceLine.calculateAffine(roadObject.curveRelativePosition.toCurveRelative1D())

        // build up solid geometrical representations
        val geometries = mutableListOf<AbstractGeometry3D>()
        geometries += Solid3DBuilder.buildCuboids(roadObject, curveAffine, parameters.numberTolerance).handleMessageList { messageList += it }
        geometries += Solid3DBuilder.buildCylinders(roadObject, curveAffine, parameters.numberTolerance).handleMessageList { messageList += it }
        geometries += Solid3DBuilder.buildPolyhedronsByRoadCorners(roadObject, roadReferenceLine, parameters.numberTolerance).handleMessageList { messageList += it }
        geometries += Solid3DBuilder.buildPolyhedronsByLocalCorners(roadObject, curveAffine, parameters.numberTolerance).handleMessageList { messageList += it }

        // build up surface geometrical representations
        geometries += Surface3DBuilder.buildRectangles(roadObject, curveAffine, parameters.numberTolerance).handleMessageList { messageList += it }
        geometries += Surface3DBuilder.buildCircles(roadObject, curveAffine, parameters.numberTolerance).handleMessageList { messageList += it }
        geometries += Surface3DBuilder.buildLinearRingsByRoadCorners(roadObject, roadReferenceLine, parameters.numberTolerance).handleMessageList { messageList += it }
        geometries += Surface3DBuilder.buildLinearRingsByLocalCorners(roadObject, curveAffine, parameters.numberTolerance).handleMessageList { messageList += it }

        roadObjectRepeat.tap {
            geometries += Solid3DBuilder.buildParametricSweeps(it, roadReferenceLine, parameters.numberTolerance).toList()
            geometries += Surface3DBuilder.buildParametricBoundedSurfacesByHorizontalRepeat(it, roadReferenceLine, parameters.numberTolerance)
            geometries += Surface3DBuilder.buildParametricBoundedSurfacesByVerticalRepeat(it, roadReferenceLine, parameters.numberTolerance)
        }

        // build up curve geometrical representations
        geometries += Curve3DBuilder.buildCurve3D(roadObject, roadReferenceLine, parameters.numberTolerance)

        // if no other geometrical representation has been found, use a point instead
        if (geometries.isEmpty())
            geometries += Vector3DBuilder.buildVector3Ds(roadObject, curveAffine, force = true)

        check(geometries.size == 1) { "Exactly one geometry must be derived." }

        return ContextMessageList(geometries.first(), messageList)
    }

    private fun buildAttributes(roadObject: OpendriveRoadObject) =
        attributes("${parameters.attributesPrefix}roadObject_") {
            attribute("type", roadObject.type.toString())
            attribute("subtype", roadObject.subtype)
            roadObject.dynamic.tap {
                attribute("dynamic", it)
            }
            roadObject.validLength.tap {
                attribute("validLength", it)
            }
            roadObject.orientation.tap {
                attribute("orientation", it.toString())
            }
        }

    private fun buildAttributes(curveRelativePosition: CurveRelativeVector3D) =
        attributes("${parameters.attributesPrefix}curveRelativePosition_") {
            attribute("curvePosition", curveRelativePosition.curvePosition)
            attribute("lateralOffset", curveRelativePosition.lateralOffset)
            attribute("heightOffset", curveRelativePosition.heightOffset)
        }

    private fun buildAttributes(rotation: Rotation3D) =
        attributes("${parameters.attributesPrefix}curveRelativeRotation_") {
            attribute("heading", rotation.heading)
            attribute("roll", rotation.roll)
            attribute("pitch", rotation.pitch)
        }

    fun buildRoadspaceObjects(
        id: RoadspaceIdentifier,
        roadSignals: RoadSignals,
        roadReferenceLine: Curve3D,
        baseAttributes: AttributeList
    ): List<RoadspaceObject> {

        return roadSignals.signal.map { buildRoadSignalsSignal(id, it, roadReferenceLine, baseAttributes) }
    }

    private fun buildRoadSignalsSignal(
        id: RoadspaceIdentifier,
        roadSignal: RoadSignalsSignal,
        roadReferenceLine: Curve3D,
        baseAttributes: AttributeList
    ): RoadspaceObject {

        val objectId = RoadspaceObjectIdentifier(roadSignal.id, roadSignal.name, id)

        val geometry = buildGeometries(roadSignal, roadReferenceLine)
        val attributes = baseAttributes +
            buildAttributes(roadSignal) +
            buildAttributes(roadSignal.curveRelativePosition) +
            buildAttributes(roadSignal.referenceLinePointRelativeRotation)

        return RoadspaceObject(objectId, RoadObjectType.SIGNAL, geometry, attributes)
    }

    private fun buildAttributes(signal: RoadSignalsSignal): AttributeList =
        attributes("${parameters.attributesPrefix}roadSignal_") {
            attribute("dynamic", signal.dynamic)
            attribute("orientation", signal.orientation.toString())
            signal.country.tap {
                attribute("countryCode", it.toString())
            }
            attribute("type", signal.type)
            attribute("subtype", signal.subtype)
            signal.value.tap {
                attribute("value", it)
            }
        }

    private fun buildGeometries(signal: RoadSignalsSignal, roadReferenceLine: Curve3D): AbstractGeometry3D {
        val curveAffine = roadReferenceLine.calculateAffine(signal.curveRelativePosition.toCurveRelative1D())

        return Vector3DBuilder.buildVector3Ds(signal, curveAffine, force = true)
    }
}
