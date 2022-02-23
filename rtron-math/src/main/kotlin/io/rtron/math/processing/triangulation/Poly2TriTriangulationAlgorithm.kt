/*
 * Copyright 2019-2022 Chair of Geoinformatics, Technical University of Munich
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

package io.rtron.math.processing.triangulation

import arrow.core.Either
import io.rtron.math.geometry.euclidean.threed.point.Vector3D
import io.rtron.math.geometry.euclidean.threed.surface.Polygon3D
import io.rtron.math.processing.calculateNormal
import io.rtron.math.processing.isColinear
import io.rtron.math.std.HALF_PI
import io.rtron.math.std.QUARTER_PI
import io.rtron.std.handleFailure
import io.rtron.std.toResult
import org.poly2tri.Poly2Tri
import org.poly2tri.geometry.polygon.Polygon as P2TPolygon
import org.poly2tri.geometry.polygon.PolygonPoint as P2TPolygonPoint

/**
 * Adapts the triangulation algorithm of [Poly2Tri](https://github.com/orbisgis/poly2tri.java).
 */
class Poly2TriTriangulationAlgorithm : TriangulationAlgorithm() {

    override fun triangulate(vertices: List<Vector3D>, tolerance: Double): Either<Exception, List<Polygon3D>> {

        val polygon = P2TPolygon(vertices.map { P2TPolygonPoint(it.x, it.y, it.z) })

        poly2TriTriangulation(polygon).toResult().handleFailure { return Either.Left(it.error) }
        val triangles = polygonBackConversion(polygon, tolerance).toResult().handleFailure { return Either.Left(it.error) }

        val adjustedTriangles = adjustOrientation(vertices, triangles)
        return Either.Right(adjustedTriangles)
    }

    /**
     * Performs the Poly2Tri triangulation, which runs on the mutable [polygon].
     *
     * @return true, if triangulation was successful
     */
    private fun poly2TriTriangulation(polygon: P2TPolygon): Either<Exception, Boolean> {
        try {
            Poly2Tri.triangulate(polygon)
        } catch (e: Exception) {
            return Either.Left(IllegalStateException("Poly2Tri-Triangulation failure: (${e.message})."))
        } catch (e: StackOverflowError) {
            return Either.Left(RuntimeException("Poly2Tri-Triangulation failure: StackOverflowError."))
        }
        return Either.Right(true)
    }

    /**
     * Converts the Poly2Tri triangulation results back to a list of [Polygon3D].
     */
    private fun polygonBackConversion(polygon: P2TPolygon, tolerance: Double):
        Either<IllegalStateException, List<Polygon3D>> {
        val triangles = polygon.triangles.map {
            val triangulatedVertices: List<Vector3D> = it.points.map { point -> Vector3D(point.x, point.y, point.z) }

            if (triangulatedVertices.isColinear(tolerance))
                return Either.Left(IllegalStateException("Triangulation failure (colinear vertices)."))
            return@map Polygon3D(triangulatedVertices, tolerance)
        }

        return Either.Right(triangles)
    }

    /**
     * As Poly2Tri ignores the rotation of the triangles, this function reintroduces the original orientation.
     * Therefore it calculates the reference normal based on the [originalVertices] and then reorients the triangles
     * accordingly, if necessary.
     *
     * @param originalVertices used for calculating the reference orientation
     * @param triangles triangles for which the orientation is to be adjusted
     * @return triangles with adjusted orientation
     */
    private fun adjustOrientation(originalVertices: List<Vector3D>, triangles: List<Polygon3D>): List<Polygon3D> {
        val referenceNormal = originalVertices.calculateNormal()
        return triangles.map {
            val triangleNormal = it.vertices.calculateNormal()
            if (referenceNormal.angle(triangleNormal) > HALF_PI + QUARTER_PI) it.reversed()
            else it
        }
    }
}
