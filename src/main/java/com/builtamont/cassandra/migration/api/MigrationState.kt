/**
 * File     : MigrationState.kt
 * License  :
 *   Original   - Copyright (c) 2010 - 2016 Boxfuse GmbH
 *   Derivative - Copyright (c) 2016 Citadel Technology Solutions Pte Ltd
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.builtamont.cassandra.migration.api

/**
 * The state of a migration.
 *
 * @param displayName The name suitable for display to the end-user.
 * @param isResolved Flag indicating if this migration is available.
 * @param isApplied Flag indicating if this migration has been applied or not.
 * @param isFailed Flag indicating if this migration has failed when it was applied or not.
 */
enum class MigrationState(
    val displayName: String,
    val isResolved: Boolean,
    val isApplied: Boolean,
    val isFailed: Boolean
) {

    /** This migration has not been applied yet. */
    PENDING("Pending", true, false, false),

    /** This migration has not been applied yet, and won't be applied because target is set to a lower version. */
    ABOVE_TARGET(">Target", true, false, false),

    /** This migration was not applied against this DB, because the metadata table was baselined with a higher version. */
    BELOW_BASELINE("<Baseln", true, false, false),

    /** This migration has baselined this DB. */
    BASELINE("Baselin", true, true, false),

    /**
     * This usually indicates a problem.
     *
     * This migration was not applied against this DB, because a migration with a higher version has already been
     * applied. This probably means some check-ins happened out of order.
     *
     * Fix by increasing the version number, run clean and migrate again or rerun migration with outOfOrder enabled.
     */
    IGNORED("Ignored", true, false, false),

    /**
     * This migration succeeded.
     *
     * This migration was applied against this DB, but it is not available locally.
     * This usually results from multiple older migration files being consolidated into a single one.
     */
    MISSING_SUCCESS("Missing", false, true, false),

    /**
     * This migration failed.
     *
     * This migration was applied against this DB, but it is not available locally.
     * This usually results from multiple older migration files being consolidated into a single one.
     *
     * This should rarely, if ever, occur in practice.
     */
    MISSING_FAILED("MisFail", false, true, true),

    /** This migration succeeded. */
    SUCCESS("Success", true, true, false),

    /** This migration failed. */
    FAILED("Failed", true, true, true),

    /**
     * This migration succeeded.
     *
     * This migration succeeded, but it was applied out of order.
     * Rerunning the entire migration history might produce different results!
     */
    OUT_OF_ORDER("OutOrdr", true, true, false),

    /**
     * This migration succeeded.
     *
     * This migration has been applied against the DB, but it is not available locally.
     * Its version is higher than the highest version available locally.
     * It was most likely successfully installed by a future version of this deployable.
     */
    FUTURE_SUCCESS("Future", false, true, false),

    /**
     * This migration failed.
     *
     * This migration has been applied against the DB, but it is not available locally.
     * Its version is higher than the highest version available locally.
     * It most likely failed during the installation of a future version of this deployable.
     */
    FUTURE_FAILED("FutFail", false, true, true),

    /** This is a repeatable migration that is outdated and should be re-applied. */
    OUTDATED("Outdate", true, true, false),

    /** This is a repeatable migration that is outdated and has already been superseded by a newer run. */
    SUPERSEDED("Superse", true, true, false)

}
