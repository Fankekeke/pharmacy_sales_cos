package cc.mrbird.febs.cos.controller;

import cc.mrbird.febs.common.utils.R;
import cc.mrbird.febs.cos.service.IPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cos/prediction")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PredictionController {

    private final IPredictionService predictionService;

    /**
     * 预测药品销售量
     */
    @GetMapping("/sales")
    public R predictSales(@RequestParam Integer drugId,
                          @RequestParam(defaultValue = "7") Integer days) {
        return R.ok(predictionService.predictSales(drugId, days));
    }

    /**
     * 预测库存需求
     */
    @GetMapping("/inventory")
    public R predictInventory(@RequestParam Integer pharmacyId,
                              @RequestParam(defaultValue = "30") Integer days) {
        return R.ok(predictionService.predictInventoryDemand(pharmacyId, days));
    }
}
