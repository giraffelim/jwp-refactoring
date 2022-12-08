package kitchenpos.application;

import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.dao.TableGroupDao;
import kitchenpos.domain.OrderStatus;
import kitchenpos.domain.OrderTable;
import kitchenpos.domain.TableGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TableGroupServiceTest {
    @Mock
    private OrderDao orderDao;

    @Mock
    private OrderTableDao orderTableDao;

    @Mock
    private TableGroupDao tableGroupDao;

    @InjectMocks
    private TableGroupService tableGroupService;

    private OrderTable firstTable;
    private OrderTable secondTable;
    private List<OrderTable> orderTables;

    @BeforeEach
    void setUp() {
        firstTable = new OrderTable(1L, null, 0, true);
        secondTable = new OrderTable(2L, null, 0, true);

        orderTables = Arrays.asList(firstTable, secondTable);
    }

    @DisplayName("테이블 그룹을 등록할 수 있다.")
    @Test
    void create() {
        TableGroup tableGroup = new TableGroup(1L, LocalDateTime.now(), orderTables);
        given(orderTableDao.findAllByIdIn(Arrays.asList(1L, 2L))).willReturn(orderTables);
        given(tableGroupDao.save(tableGroup)).willReturn(tableGroup);

        TableGroup savedTableGroup = tableGroupService.create(tableGroup);

        assertThat(savedTableGroup.getId()).isNotNull();
        assertThat(savedTableGroup.getCreatedDate()).isNotNull();
        assertThat(savedTableGroup.getOrderTables()).containsAll(orderTables);
    }

    @DisplayName("주문 테이블이 비어있다면 테이블 그룹을 등록할 수 없다.")
    @Test
    void createWithEmptyOrderTables() {
        TableGroup tableGroup = new TableGroup(1L, LocalDateTime.now(), Collections.emptyList());

        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문 테이블이 2개보다 작을 경우 테이블 그룹을 등록할 수 없다.")
    @Test
    void createWithOrderTablesMinCriteria() {
        TableGroup tableGroup = new TableGroup(1L, LocalDateTime.now(), Collections.singletonList(firstTable));

        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문 테이블이 등록되지 않은 경우 테이블 그룹을 등록할 수 없다.")
    @Test
    void createWithNotExistOrderTables() {
        TableGroup tableGroup = new TableGroup(1L, LocalDateTime.now(), orderTables);
        given(orderTableDao.findAllByIdIn(Arrays.asList(1L, 2L))).willReturn(Collections.emptyList());

        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문 테이블이 이미 이용중인 경우 테이블 그룹을 등록할 수 없다.")
    @Test
    void createWithNotEmptyOrderTable() {
        firstTable.setEmpty(false);
        TableGroup tableGroup = new TableGroup(1L, LocalDateTime.now(), orderTables);
        given(orderTableDao.findAllByIdIn(Arrays.asList(1L, 2L))).willReturn(orderTables);

        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("주문 테이블이 이미 테이블 그룹으로 등록되어 있는 경우 테이블 그룹을 등록할 수 없다.")
    @Test
    void createWithAlreadyTableGroup() {
        firstTable.setTableGroupId(1L);
        TableGroup tableGroup = new TableGroup(1L, LocalDateTime.now(), orderTables);
        given(orderTableDao.findAllByIdIn(Arrays.asList(1L, 2L))).willReturn(orderTables);

        assertThatThrownBy(() -> tableGroupService.create(tableGroup))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("테이블 그룹을 해제할 수 있다.")
    @Test
    void ungroup() {
        given(orderTableDao.findAllByTableGroupId(1L)).willReturn(orderTables);

        tableGroupService.ungroup(1L);

        assertThat(firstTable.getTableGroupId()).isNull();
        assertThat(secondTable.getTableGroupId()).isNull();
    }

    @DisplayName("주문 상태가 조리 또는 식사중이라면 테이블 그룹을 해제할 수 없다.")
    @Test
    void ungroupWithOrderStatus() {
        List<String> orderStatus = Arrays.asList(OrderStatus.COOKING.name(), OrderStatus.MEAL.name());
        given(orderTableDao.findAllByTableGroupId(1L)).willReturn(orderTables);
        given(orderDao.existsByOrderTableIdInAndOrderStatusIn(Arrays.asList(1L, 2L), orderStatus)).willReturn(true);

        assertThatThrownBy(() -> tableGroupService.ungroup(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}