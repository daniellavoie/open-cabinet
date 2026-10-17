package dev.daniellavoie.opencabinet.template.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dev.daniellavoie.opencabinet.member.domain.model.Member;
import dev.daniellavoie.opencabinet.member.domain.port.api.MemberDomain;
import dev.daniellavoie.opencabinet.project.domain.model.Cabinet;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetTemplate;
import dev.daniellavoie.opencabinet.project.domain.model.CabinetType;
import dev.daniellavoie.opencabinet.project.domain.model.FrontKind;
import dev.daniellavoie.opencabinet.project.domain.model.FrontRow;
import dev.daniellavoie.opencabinet.project.domain.port.api.ProjectDomain;
import dev.daniellavoie.opencabinet.template.domain.model.ShopCabinetTemplate;
import dev.daniellavoie.opencabinet.template.domain.model.ShopCabinetTemplateRequest;
import dev.daniellavoie.opencabinet.template.domain.port.backend.ShopCabinetTemplateRepository;

class DefaultShopCabinetTemplateDomainTest {
	private static final UUID SHOP_ID = UUID.randomUUID();
	private static final CabinetTemplate BASE_DOOR = CabinetTemplate.builder()
			.code("BASE_DOOR").version(1).type(CabinetType.BASE)
			.minWidthMm(228.6).maxWidthMm(914.4).minHeightMm(800).maxHeightMm(950)
			.minDepthMm(300).maxDepthMm(700).maxShelfCount(4)
			.frontRows(List.of(FrontRow.builder().kind(FrontKind.DOOR).build()))
			.build();

	private final ShopCabinetTemplateRepository repository = mock(ShopCabinetTemplateRepository.class);
	private final MemberDomain memberDomain = mock(MemberDomain.class);
	private final ProjectDomain projectDomain = mock(ProjectDomain.class);
	private final DefaultShopCabinetTemplateDomain domain = new DefaultShopCabinetTemplateDomain(repository,
			memberDomain, projectDomain);
	private final Principal owner = () -> "owner";

	@BeforeEach
	void setUp() {
		when(memberDomain.currentOwner(owner)).thenReturn(Member.builder().shopId(SHOP_ID).build());
		when(projectDomain.templates()).thenReturn(List.of(BASE_DOOR));
		when(repository.save(any(), eq(SHOP_ID))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void savesTheCabinetAsAPresetOfItsBaseTemplate() {
		var cabinet = cabinet(762).toBuilder().dividerPositionsMm(List.of(381d)).build();

		var saved = domain.create(ShopCabinetTemplateRequest.builder().name(" Vanité 30 ").cabinet(cabinet).build(),
				owner);

		assertThat(saved.getName()).isEqualTo("Vanité 30");
		assertThat(saved.getBaseCode()).isEqualTo("BASE_DOOR");
		var captor = ArgumentCaptor.forClass(ShopCabinetTemplate.class);
		verify(repository).save(captor.capture(), eq(SHOP_ID));
		var preset = captor.getValue().getPreset();
		assertThat(preset.getId()).isNull();
		assertThat(preset.getWallId()).isNull();
		assertThat(preset.getOffsetMm()).isZero();
		assertThat(preset.getWidthMm()).isEqualTo(762);
		assertThat(preset.getDividerPositionsMm()).containsExactly(381d);
	}

	@Test
	void refusesACabinetThatBreaksTheConstructionRules() {
		assertThatThrownBy(() -> domain.create(ShopCabinetTemplateRequest.builder().name("Trop large")
				.cabinet(cabinet(1200)).build(), owner))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("WIDTH_OUT_OF_RANGE");
		verify(repository, never()).save(any(), any());
	}

	private static Cabinet cabinet(double widthMm) {
		return Cabinet.builder().id("c1").templateCode("BASE_DOOR").templateVersion(1).wallId("w1").offsetMm(500).widthMm(widthMm)
				.heightMm(876.3).depthMm(609.6).shelfCount(1).build();
	}
}
