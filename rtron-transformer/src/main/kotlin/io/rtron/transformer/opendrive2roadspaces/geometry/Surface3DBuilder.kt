/*
 * Copyright 2019-2020 Chair of Geoinformatics, Technical University of Munich
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

package io.rtron.transformer.opendrive2roadspaces.geometry

import com.github.kittinunf.result.Result
import io.rtron.io.logging.Logger
import io.rtron.math.analysis.function.univariate.combination.StackedFunction
import io.rtron.math.geometry.euclidean.threed.curve.Curve3D
import io.rtron.math.geometry.euclidean.threed.point.Vector3D
import io.rtron.math.geometry.euclidean.threed.surface.Circle3D
import io.rtron.math.geometry.euclidean.threed.surface.LinearRing3D
import io.rtron.math.geometry.euclidean.threed.surface.ParametricBoundedSurface3D
import io.rtron.math.geometry.euclidean.threed.surface.Rectangle3D
import io.rtron.math.processing.LinearRing3DFactory
import io.rtron.math.transform.Affine3D
import io.rtron.math.transform.AffineSequence3D
import io.rtron.model.opendrive.road.objects.RoadObjectsObject
import io.rtron.model.opendrive.road.objects.RoadObjectsObjectOutlinesOutline
import io.rtron.model.opendrive.road.objects.RoadObjectsObjectOutlinesOutlineCornerRoad
import io.rtron.model.opendrive.road.objects.RoadObjectsObjectRepeat
import io.rtron.model.roadspaces.roadspace.objects.RoadspaceObjectIdentifier
import io.rtron.std.ContextMessage
import io.rtron.std.handleAndRemoveFailure
import io.rtron.std.handleFailure
import io.rtron.std.handleMessage
import io.rtron.transformer.opendrive2roadspaces.analysis.FunctionBuilder
import io.rtron.transformer.opendrive2roadspaces.parameter.Opendrive2RoadspacesParameters

/**
 * Builder for surface geometries in 3D from the OpenDRIVE data model.
 */
class Surface3DBuilder(
    private val reportLogger: Logger,
    private val parameters: Opendrive2RoadspacesParameters
) {

    // Properties and Initializers
    private val _functionBuilder = FunctionBuilder(reportLogger, parameters)
    private val _curve2DBuilder = Curve2DBuilder(reportLogger, parameters)

    // Methods

    /**
     * Builds a list of rectangles from the OpenDRIVE road object class ([RoadObjectsObject]) directly or from the
     * repeated entries defined in [RoadObjectsObjectRepeat].
     */
    fun buildRectangles(roadObject: RoadObjectsObject, curveAffine: Affine3D): List<Rectangle3D> {
        val rectangleList = mutableListOf<Rectangle3D>()

        if (roadObject.isRectangle()) {
            val objectAffine = Affine3D.of(roadObject.referenceLinePointRelativePose)
            val affineSequence = AffineSequence3D.of(curveAffine, objectAffine)
            rectangleList += Rectangle3D(roadObject.length, roadObject.width, parameters.tolerance, affineSequence)
        }

        if (roadObject.repeat.isRepeatedCuboid())
            this.reportLogger.infoOnce("Geometry RepeatedRectangle not implemented yet.")

        return rectangleList
    }

    /**
     * Builds a list of circles from the OpenDRIVE road object class ([RoadObjectsObject]) directly or from the
     * repeated entries defined in [RoadObjectsObjectRepeat].
     */
    fun buildCircles(roadObject: RoadObjectsObject, curveAffine: Affine3D): List<Circle3D> {
        val circleList = mutableListOf<Circle3D>()

        if (roadObject.isCircle()) {
            val objectAffine = Affine3D.of(roadObject.referenceLinePointRelativePose)
            val affineSequence = AffineSequence3D.of(curveAffine, objectAffine)
            circleList += Circle3D(roadObject.radius, parameters.tolerance, affineSequence)
        }

        if (roadObject.repeat.isRepeatCylinder())
            this.reportLogger.infoOnce("Geometry RepeatedCircle not implemented yet.")

        return circleList
    }

    /**
     * Builds a list of linear rings from an OpenDRIVE road object defined by road corner outlines.
     */
    fun buildLinearRingsByRoadCorners(
        id: RoadspaceObjectIdentifier,
        roadObject: RoadObjectsObject,
        referenceLine: Curve3D
    ): List<LinearRing3D> {

        return roadObject.getLinearRingsDefinedByRoadCorners()
            .map { buildLinearRingByRoadCorners(it, referenceLine) }
            .handleAndRemoveFailure { reportLogger.log(it, id.toString()) }
            .handleMessage { reportLogger.log(it, id.toString()) }
    }

    /**
     * Builds a single linear ring from an OpenDRIVE road object defined by road corner outlines.
     */
    private fun buildLinearRingByRoadCorners(outline: RoadObjectsObjectOutlinesOutline, referenceLine: Curve3D):
        Result<ContextMessage<LinearRing3D>, IllegalArgumentException> {

            val vertices = outline.cornerRoad
                .map { buildVertices(it, referenceLine) }
                .handleAndRemoveFailure { reportLogger.log(it) }

            return LinearRing3DFactory.buildFromVertices(vertices, parameters.tolerance)
        }

    /**
     * Builds a vertex from the OpenDRIVE road corner element.
     */
    private fun buildVertices(cornerRoad: RoadObjectsObjectOutlinesOutlineCornerRoad, referenceLine: Curve3D):
        Result<Vector3D, Exception> {
            val affine = referenceLine.calculateAffine(cornerRoad.curveRelativePosition)
                .handleFailure { return it }
            val basePoint = cornerRoad.getBasePoint()
                .handleFailure { return it }
                .let { affine.transform(it.getCartesianCurveOffset()) }
            return Result.success(basePoint)
        }

    /**
     * Builds a list of linear rings from an OpenDRIVE road object defined by local corner outlines.
     */
    fun buildLinearRingsByLocalCorners(
        id: RoadspaceObjectIdentifier,
        roadObject: RoadObjectsObject,
        curveAffine: Affine3D
    ): List<LinearRing3D> {
        val objectAffine = Affine3D.of(roadObject.referenceLinePointRelativePose)
        val affineSequence = AffineSequence3D.of(curveAffine, objectAffine)

        return roadObject.getLinearRingsDefinedByLocalCorners()
            .map { buildLinearRingByLocalCorners(id, it) }
            .handleAndRemoveFailure { reportLogger.log(it, id.toString()) }
            .handleMessage { reportLogger.log(it, id.toString()) }
            .map { it.copy(affineSequence = affineSequence) }
    }

    /**
     * Builds a single linear ring from an OpenDRIVE road object defined by local corner outlines.
     */
    private fun buildLinearRingByLocalCorners(id: RoadspaceObjectIdentifier, outline: RoadObjectsObjectOutlinesOutline):
        Result<ContextMessage<LinearRing3D>, IllegalArgumentException> {

            val vertices = outline.cornerLocal
                .map { it.getBasePoint() }
                .handleAndRemoveFailure { reportLogger.log(it, id.toString(), "Removing outline point.") }

            return LinearRing3DFactory.buildFromVertices(vertices, parameters.tolerance)
        }

    /**
     * Builds a parametric bounded surface from OpenDRIVE road objects defined by repeat entries representing a horizontal surface.
     */
    fun buildParametricBoundedSurfacesByHorizontalRepeat(
        id: RoadspaceObjectIdentifier,
        roadObjectRepeat: RoadObjectsObjectRepeat,
        roadReferenceLine: Curve3D
    ): List<ParametricBoundedSurface3D> {
        if (!roadObjectRepeat.isHorizontalParametricBoundedSurface()) return emptyList()

        // curve over which the object is moved
        val objectReferenceCurve2D =
            _curve2DBuilder.buildLateralTranslatedCurve(roadObjectRepeat, roadReferenceLine)
                .handleFailure { reportLogger.log(it, id.toString(), "Removing object."); return emptyList() }
        val objectReferenceHeight =
            _functionBuilder.buildStackedHeightFunctionFromRepeat(roadObjectRepeat, roadReferenceLine)

        // dimension of the object
        val widthFunction = roadObjectRepeat.getObjectWidthFunction()

        // absolute boundary curves
        val leftBoundaryCurve2D = objectReferenceCurve2D.addLateralTranslation(widthFunction, -0.5)
        val leftBoundary = Curve3D(leftBoundaryCurve2D, objectReferenceHeight)
        val rightBoundaryCurve2D = objectReferenceCurve2D.addLateralTranslation(widthFunction, +0.5)
        val rightBoundary = Curve3D(rightBoundaryCurve2D, objectReferenceHeight)

        val parametricBoundedSurface = ParametricBoundedSurface3D(leftBoundary, rightBoundary, parameters.tolerance, ParametricBoundedSurface3D.DEFAULT_STEP_SIZE)
        return listOf(parametricBoundedSurface)
    }

    /**
     * Builds a parametric bounded surface from OpenDRIVE road objects defined by repeat entries representing a vertical surface.
     */
    fun buildParametricBoundedSurfacesByVerticalRepeat(
        id: RoadspaceObjectIdentifier,
        roadObjectRepeat: RoadObjectsObjectRepeat,
        roadReferenceLine: Curve3D
    ): List<ParametricBoundedSurface3D> {
        if (!roadObjectRepeat.isVerticalParametricBoundedSurface()) return emptyList()

        // curve over which the object is moved
        val objectReferenceCurve2D =
            _curve2DBuilder.buildLateralTranslatedCurve(roadObjectRepeat, roadReferenceLine)
                .handleFailure { reportLogger.log(it, id.toString(), "Removing object."); return emptyList() }
        val objectReferenceHeight =
            _functionBuilder.buildStackedHeightFunctionFromRepeat(roadObjectRepeat, roadReferenceLine)

        // dimension of the object
        val heightFunction = roadObjectRepeat.getObjectHeightFunction()

        // absolute boundary curves
        val lowerBoundary = Curve3D(objectReferenceCurve2D, objectReferenceHeight)
        val upperBoundaryHeight = StackedFunction.ofSum(objectReferenceHeight, heightFunction, defaultValue = 0.0)
        val upperBoundary = Curve3D(objectReferenceCurve2D, upperBoundaryHeight)

        val parametricBoundedSurface = ParametricBoundedSurface3D(lowerBoundary, upperBoundary, parameters.tolerance, ParametricBoundedSurface3D.DEFAULT_STEP_SIZE)
        return listOf(parametricBoundedSurface)
    }
}
