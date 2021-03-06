/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.dashboard;

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.conditional.AirplaneModeCondition;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DashboardDataTest {
    private static final String TEST_SUGGESTION_TITLE = "Use fingerprint";
    private static final String TEST_CATEGORY_TILE_TITLE = "Display";

    private DashboardData mDashboardDataWithOneConditions;
    private DashboardData mDashboardDataWithTwoConditions;
    private DashboardData mDashboardDataWithNoItems;
    @Mock
    private Tile mTestCategoryTile;
    @Mock
    private Tile mTestSuggestion;
    @Mock
    private DashboardCategory mDashboardCategory;
    @Mock
    private Condition mTestCondition;
    @Mock
    private Condition mSecondCondition; // condition used to test insert in DiffUtil

    @Before
    public void SetUp() {
        MockitoAnnotations.initMocks(this);

        // Build suggestions
        final List<Tile> suggestions = new ArrayList<>();
        mTestSuggestion.title = TEST_SUGGESTION_TITLE;
        suggestions.add(mTestSuggestion);

        // Build oneItemConditions
        final List<Condition> oneItemConditions = new ArrayList<>();
        when(mTestCondition.shouldShow()).thenReturn(true);
        oneItemConditions.add(mTestCondition);

        // Build twoItemConditions
        final List<Condition> twoItemsConditions = new ArrayList<>();
        when(mSecondCondition.shouldShow()).thenReturn(true);
        twoItemsConditions.add(mTestCondition);
        twoItemsConditions.add(mSecondCondition);

        // Build categories
        final List<DashboardCategory> categories = new ArrayList<>();
        mTestCategoryTile.title = TEST_CATEGORY_TILE_TITLE;
        mDashboardCategory.title = "test";
        mDashboardCategory.tiles = new ArrayList<>();
        mDashboardCategory.tiles.add(mTestCategoryTile);
        categories.add(mDashboardCategory);

        // Build DashboardData 
        mDashboardDataWithOneConditions = new DashboardData.Builder()
                .setConditions(oneItemConditions)
                .setCategories(categories)
                .setSuggestions(suggestions)
                .build();

        mDashboardDataWithTwoConditions = new DashboardData.Builder()
                .setConditions(twoItemsConditions)
                .setCategories(categories)
                .setSuggestions(suggestions)
                .build();

        mDashboardDataWithNoItems = new DashboardData.Builder()
                .setConditions(null)
                .setCategories(null)
                .setSuggestions(null)
                .build();
    }

    @Test
    public void testBuildItemsData_containsAllData() {
        final DashboardData.SuggestionHeaderData data =
                new DashboardData.SuggestionHeaderData(false, 1, 0);
        final Object[] expectedObjects = {null, mTestCondition, null, data, mTestSuggestion,
                mDashboardCategory, mTestCategoryTile};
        final int expectedSize = expectedObjects.length;

        assertThat(mDashboardDataWithOneConditions.getItemList().size())
                .isEqualTo(expectedSize);
        for (int i = 0; i < expectedSize; i++) {
            if (mDashboardDataWithOneConditions.getItemEntityByPosition(i)
                    instanceof DashboardData.SuggestionHeaderData) {
                // SuggestionHeaderData is created inside when build, we can only use isEqualTo
                assertThat(mDashboardDataWithOneConditions.getItemEntityByPosition(i))
                        .isEqualTo(expectedObjects[i]);
            } else {
                assertThat(mDashboardDataWithOneConditions.getItemEntityByPosition(i))
                        .isSameAs(expectedObjects[i]);
            }
        }
    }

    @Test
    public void testGetPositionByEntity_selfInstance_returnPositionFound() {
        final int position = mDashboardDataWithOneConditions
                .getPositionByEntity(mTestCondition);
        assertThat(position).isNotEqualTo(DashboardData.POSITION_NOT_FOUND);
    }

    @Test
    public void testGetPositionByEntity_notExisted_returnNotFound() {
        final Condition condition = mock(AirplaneModeCondition.class);
        final int position = mDashboardDataWithOneConditions.getPositionByEntity(condition);
        assertThat(position).isEqualTo(DashboardData.POSITION_NOT_FOUND);
    }

    @Test
    public void testGetPositionByTile_selfInstance_returnPositionFound() {
        final int position = mDashboardDataWithOneConditions
                .getPositionByTile(mTestCategoryTile);
        assertThat(position).isNotEqualTo(DashboardData.POSITION_NOT_FOUND);
    }

    @Test
    public void testGetPositionByTile_equalTitle_returnPositionFound() {
        final Tile tile = mock(Tile.class);
        tile.title = TEST_CATEGORY_TILE_TITLE;
        final int position = mDashboardDataWithOneConditions.getPositionByTile(tile);
        assertThat(position).isNotEqualTo(DashboardData.POSITION_NOT_FOUND);
    }

    @Test
    public void testGetPositionByTile_notExisted_returnNotFound() {
        final Tile tile = mock(Tile.class);
        tile.title = "";
        final int position = mDashboardDataWithOneConditions.getPositionByTile(tile);
        assertThat(position).isEqualTo(DashboardData.POSITION_NOT_FOUND);
    }

    @Test
    public void testDiffUtil_DataEqual_noResultData() {
        List<ListUpdateResult.ResultData> testResultData = new ArrayList<>();
        testDiffUtil(mDashboardDataWithOneConditions,
                mDashboardDataWithOneConditions, testResultData);
    }

    @Test
    public void testDiffUtil_InsertOneCondition_ResultDataOneInserted() {
        //Build testResultData
        final List<ListUpdateResult.ResultData> testResultData = new ArrayList<>();
        testResultData.add(new ListUpdateResult.ResultData(
                ListUpdateResult.ResultData.TYPE_OPERATION_INSERT, 2, 1));

        testDiffUtil(mDashboardDataWithOneConditions,
                mDashboardDataWithTwoConditions, testResultData);
    }

    @Test
    public void testDiffUtil_DeleteAllData_ResultDataOneDeleted() {
        //Build testResultData
        final List<ListUpdateResult.ResultData> testResultData = new ArrayList<>();
        testResultData.add(new ListUpdateResult.ResultData(
                ListUpdateResult.ResultData.TYPE_OPERATION_REMOVE, 1, 6));

        testDiffUtil(mDashboardDataWithOneConditions, mDashboardDataWithNoItems, testResultData);
    }

    @Test
    public void testPayload_ItemConditionCard_returnNotNull() {
        final DashboardData.ItemsDataDiffCallback callback = new DashboardData
                .ItemsDataDiffCallback(
                mDashboardDataWithOneConditions.getItemList(),
                mDashboardDataWithOneConditions.getItemList());

        // Item in position 1 is condition card, which payload should not be null
        assertThat(callback.getChangePayload(1, 1)).isNotNull();
    }

    @Test
    public void testPayload_ItemNotConditionCard_returnNull() {
        final DashboardData.ItemsDataDiffCallback callback = new DashboardData
                .ItemsDataDiffCallback(
                mDashboardDataWithOneConditions.getItemList(),
                mDashboardDataWithOneConditions.getItemList());

        // Position 0 is spacer, 1 is condition card, so others' payload should be null
        for (int i = 2; i < mDashboardDataWithOneConditions.getItemList().size(); i++) {
            assertThat(callback.getChangePayload(i, i)).isNull();
        }

    }

    /**
     * Test when using the
     * {@link com.android.settings.dashboard.DashboardData.ItemsDataDiffCallback}
     * to transfer List from {@paramref baseDashboardData} to {@paramref diffDashboardData}, whether
     * the transform data result is equals to {@paramref testResultData}
     * <p>
     * The steps are described below:
     * 1. Calculate a {@link android.support.v7.util.DiffUtil.DiffResult} from
     * {@paramref baseDashboardData} to {@paramref diffDashboardData}
     * <p>
     * 2. Dispatch the {@link android.support.v7.util.DiffUtil.DiffResult} calculated from step 1
     * into {@link ListUpdateResult}
     * <p>
     * 3. Get result data(a.k.a. baseResultData) from {@link ListUpdateResult} and compare it to
     * {@paramref testResultData}
     * <p>
     * Because baseResultData and {@paramref testResultData} don't have sequence. When do the
     * comparison, we will sort them first and then compare the inside data from them one by one.
     *
     * @param baseDashboardData
     * @param diffDashboardData
     * @param testResultData
     */
    private void testDiffUtil(DashboardData baseDashboardData, DashboardData diffDashboardData,
            List<ListUpdateResult.ResultData> testResultData) {
        final DiffUtil.DiffResult diffUtilResult = DiffUtil.calculateDiff(
                new DashboardData.ItemsDataDiffCallback(
                        baseDashboardData.getItemList(), diffDashboardData.getItemList()));

        // Dispatch to listUpdateResult, then listUpdateResult will have result data
        final ListUpdateResult listUpdateResult = new ListUpdateResult();
        diffUtilResult.dispatchUpdatesTo(listUpdateResult);

        final List<ListUpdateResult.ResultData> baseResultData = listUpdateResult.getResultData();
        assertThat(testResultData.size()).isEqualTo(baseResultData.size());

        // Sort them so we can compare them one by one using a for loop
        Collections.sort(baseResultData);
        Collections.sort(testResultData);
        final int size = baseResultData.size();
        for (int i = 0; i < size; i++) {
            // Refer to equals method in ResultData
            assertThat(baseResultData.get(i)).isEqualTo(testResultData.get(i));
        }
    }

    /**
     * This class contains the result about how the changes made to convert one
     * list to another list. It implements ListUpdateCallback to record the result data.
     */
    private static class ListUpdateResult implements ListUpdateCallback {
        final private List<ResultData> mResultData;

        public ListUpdateResult() {
            mResultData = new ArrayList<>();
        }

        public List<ResultData> getResultData() {
            return mResultData;
        }

        @Override
        public void onInserted(int position, int count) {
            mResultData.add(new ResultData(ResultData.TYPE_OPERATION_INSERT, position, count));
        }

        @Override
        public void onRemoved(int position, int count) {
            mResultData.add(new ResultData(ResultData.TYPE_OPERATION_REMOVE, position, count));
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            mResultData.add(
                    new ResultData(ResultData.TYPE_OPERATION_MOVE, fromPosition, toPosition));
        }

        @Override
        public void onChanged(int position, int count, Object payload) {
            mResultData.add(new ResultData(ResultData.TYPE_OPERATION_CHANGE, position, count));
        }

        /**
         * This class contains general type and field to record the operation data generated
         * in {@link ListUpdateCallback}. Please refer to {@link ListUpdateCallback} for more info.
         * <p>
         * The following are examples about the data stored in this class:
         * <p>
         * "The data starts from position(arg1) with count number(arg2) is changed(operation)"
         * or "The data is moved(operation) from position1(arg1) to position2(arg2)"
         */
        private static class ResultData implements Comparable<ResultData> {
            public static final int TYPE_OPERATION_INSERT = 0;
            public static final int TYPE_OPERATION_REMOVE = 1;
            public static final int TYPE_OPERATION_MOVE = 2;
            public static final int TYPE_OPERATION_CHANGE = 3;

            public final int operation;
            public final int arg1;
            public final int arg2;

            public ResultData(int operation, int arg1, int arg2) {
                this.operation = operation;
                this.arg1 = arg1;
                this.arg2 = arg2;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }

                if (!(obj instanceof ResultData)) {
                    return false;
                }

                ResultData targetData = (ResultData) obj;

                return operation == targetData.operation && arg1 == targetData.arg1
                        && arg2 == targetData.arg2;
            }

            @Override
            public int compareTo(@NonNull ResultData resultData) {
                if (this.operation != resultData.operation) {
                    return operation - resultData.operation;
                }

                if (arg1 != resultData.arg1) {
                    return arg1 - resultData.arg1;
                }

                return arg2 - resultData.arg2;
            }

            @Override
            public String toString() {
                return "op:" + operation + ",arg1:" + arg1 + ",arg2:" + arg2;
            }
        }
    }
}
